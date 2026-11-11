package com.pawmotionai.health

import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PetAccess
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

@Service
class HealthService(private val jdbc: JdbcClient, private val petAccess: PetAccess) {

    companion object {
        const val DUE_SOON_DAYS = 30L
        private val random = SecureRandom()

        fun status(dueOn: LocalDate?, today: LocalDate): HealthStatus = when {
            dueOn == null -> HealthStatus.DONE
            dueOn.isBefore(today) -> HealthStatus.OVERDUE
            !dueOn.isAfter(today.plusDays(DUE_SOON_DAYS)) -> HealthStatus.DUE_SOON
            else -> HealthStatus.UPCOMING
        }

        /** Reminders a week before and on the due date, at 12:00 UTC, skipping times already past. */
        fun reminderTimes(dueOn: LocalDate?, now: Instant): List<Instant> = if (dueOn == null) emptyList() else
            listOf(dueOn.minusDays(7), dueOn).map { it.atTime(12, 0).toInstant(ZoneOffset.UTC) }.filter { it.isAfter(now) }

        fun hash(token: String): String =
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Transactional(readOnly = true)
    fun passport(ownerId: UUID, petId: UUID): HealthPassportDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        return load(petId)
    }

    @Transactional
    fun addRecord(ownerId: UUID, petId: UUID, request: HealthRecordRequest): HealthRecordDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        validate(request)
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO health_records (id, pet_id, owner_id, type, name, administered_on, due_on, vet_name, clinic_name, notes, created_at, updated_at)
            VALUES (:id, :pet, :owner, :type, :name, :administered, :due, :vet, :clinic, :notes, now(), now())
            """,
        ).param("id", id).param("pet", petId).param("owner", ownerId).bind(request).update()
        scheduleReminders(id, ownerId, request.dueOn)
        return record(petId, id)
    }

    @Transactional
    fun updateRecord(ownerId: UUID, petId: UUID, recordId: UUID, request: HealthRecordRequest): HealthRecordDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        validate(request)
        val updated = jdbc.sql(
            """
            UPDATE health_records SET type = :type, name = :name, administered_on = :administered, due_on = :due, vet_name = :vet,
                   clinic_name = :clinic, notes = :notes, updated_at = now(), version = version + 1
            WHERE id = :id AND pet_id = :pet
            """,
        ).param("id", recordId).param("pet", petId).bind(request).update()
        if (updated == 0) throw NotFoundException("Health record not found")
        jdbc.sql("DELETE FROM health_reminders WHERE record_id = :id AND sent_at IS NULL").param("id", recordId).update()
        scheduleReminders(recordId, ownerId, request.dueOn)
        return record(petId, recordId)
    }

    @Transactional
    fun deleteRecord(ownerId: UUID, petId: UUID, recordId: UUID) {
        petAccess.requireOwnedActivePet(ownerId, petId)
        if (jdbc.sql("DELETE FROM health_records WHERE id = :id AND pet_id = :pet").param("id", recordId).param("pet", petId).update() == 0) {
            throw NotFoundException("Health record not found")
        }
    }

    @Transactional
    fun recordWeight(ownerId: UUID, petId: UUID, request: WeightRequest): List<WeightDto> {
        petAccess.requireOwnedActivePet(ownerId, petId)
        if (request.measuredOn!!.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))) throw BadRequestException("measuredOn cannot be in the future")
        jdbc.sql(
            """
            INSERT INTO pet_weights (id, pet_id, measured_on, weight_kg, created_at) VALUES (:id, :pet, :on, :kg, now())
            ON CONFLICT (pet_id, measured_on) DO UPDATE SET weight_kg = EXCLUDED.weight_kg
            """,
        ).param("id", UUID.randomUUID()).param("pet", petId).param("on", request.measuredOn).param("kg", request.weightKg).update()
        return weights(petId)
    }

    @Transactional
    fun createShareLink(ownerId: UUID, petId: UUID, days: Int): ShareLinkDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
        val expiresAt = Instant.now().plus(Duration.ofDays(days.toLong()))
        jdbc.sql(
            "INSERT INTO vet_share_links (id, pet_id, owner_id, token_hash, expires_at, created_at) VALUES (:id, :pet, :owner, :hash, :expires, now())",
        ).param("id", UUID.randomUUID()).param("pet", petId).param("owner", ownerId).param("hash", hash(token))
            .param("expires", Timestamp.from(expiresAt)).update()
        return ShareLinkDto(token, "/vet/$token", expiresAt)
    }

    @Transactional
    fun revokeShareLinks(ownerId: UUID, petId: UUID) {
        petAccess.requireOwnedActivePet(ownerId, petId)
        jdbc.sql("UPDATE vet_share_links SET revoked_at = now() WHERE pet_id = :pet AND revoked_at IS NULL").param("pet", petId).update()
    }

    @Transactional(readOnly = true)
    fun sharedPassport(token: String): HealthPassportDto {
        val petId = jdbc.sql(
            """
            SELECT l.pet_id FROM vet_share_links l JOIN pets p ON p.id = l.pet_id AND p.status = 'ACTIVE'
            WHERE l.token_hash = :hash AND l.revoked_at IS NULL AND l.expires_at > now()
            """,
        ).param("hash", hash(token)).query(UUID::class.java).optional().orElseThrow { NotFoundException("Share link is invalid or expired") }
        return load(petId)
    }

    private fun load(petId: UUID): HealthPassportDto {
        val today = LocalDate.now(ZoneOffset.UTC)
        val records = jdbc.sql("SELECT * FROM health_records WHERE pet_id = :pet ORDER BY COALESCE(due_on, administered_on) DESC, id")
            .param("pet", petId).query { rs, _ -> record(rs, today) }.list()
        data class Pet(val name: String, val species: String, val breed: String?)
        val pet = jdbc.sql("SELECT name, lower(species) AS species, breed FROM pets WHERE id = :id").param("id", petId)
            .query { rs, _ -> Pet(rs.getString(1), rs.getString(2), rs.getString(3)) }.single()
        val upcoming = records.filter { it.status != HealthStatus.DONE && it.status != HealthStatus.UPCOMING }.sortedBy { it.dueOn }
        return HealthPassportDto(petId, pet.name, pet.species, pet.breed, records, upcoming, weights(petId))
    }

    private fun weights(petId: UUID): List<WeightDto> =
        jdbc.sql("SELECT measured_on, weight_kg FROM pet_weights WHERE pet_id = :pet ORDER BY measured_on").param("pet", petId)
            .query { rs, _ -> WeightDto(rs.getObject(1, LocalDate::class.java), rs.getBigDecimal(2)) }.list()

    private fun record(petId: UUID, id: UUID): HealthRecordDto =
        jdbc.sql("SELECT * FROM health_records WHERE id = :id AND pet_id = :pet").param("id", id).param("pet", petId)
            .query { rs, _ -> record(rs, LocalDate.now(ZoneOffset.UTC)) }.single()

    private fun record(rs: ResultSet, today: LocalDate): HealthRecordDto {
        val due = rs.getObject("due_on", LocalDate::class.java)
        return HealthRecordDto(
            rs.getObject("id", UUID::class.java), HealthRecordType.valueOf(rs.getString("type")), rs.getString("name"),
            rs.getObject("administered_on", LocalDate::class.java), due, rs.getString("vet_name"), rs.getString("clinic_name"),
            rs.getString("notes"), status(due, today),
        )
    }

    private fun scheduleReminders(recordId: UUID, ownerId: UUID, dueOn: LocalDate?) {
        reminderTimes(dueOn, Instant.now()).forEach { at ->
            jdbc.sql(
                "INSERT INTO health_reminders (id, record_id, owner_id, remind_at, created_at) VALUES (:id, :record, :owner, :at, now()) ON CONFLICT DO NOTHING",
            ).param("id", UUID.randomUUID()).param("record", recordId).param("owner", ownerId).param("at", Timestamp.from(at)).update()
        }
    }

    private fun validate(request: HealthRecordRequest) {
        if (request.administeredOn == null && request.dueOn == null) throw BadRequestException("administeredOn or dueOn is required")
        if (request.administeredOn?.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1)) == true) {
            throw BadRequestException("administeredOn cannot be in the future")
        }
    }

    private fun JdbcClient.StatementSpec.bind(request: HealthRecordRequest) = this
        .param("type", request.type!!.name).param("name", request.name!!.trim()).param("administered", request.administeredOn)
        .param("due", request.dueOn).param("vet", request.vetName?.trim()?.ifEmpty { null })
        .param("clinic", request.clinicName?.trim()?.ifEmpty { null }).param("notes", request.notes?.trim()?.ifEmpty { null })
}
