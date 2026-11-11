package com.pawmotionai.booking

import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.PetAccess
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class BookingService(private val jdbc: JdbcClient, private val petAccess: PetAccess) {

    private val log = LoggerFactory.getLogger(BookingService::class.java)

    companion object {
        private const val BOOKING_SELECT = """
            SELECT b.*, o.display_name AS owner_name, p.name AS pet_name, pp.business_name AS partner_name,
                   s.name AS service_name, s.price_cents, s.currency, s.duration_minutes
            FROM bookings b
            JOIN owners o ON o.id = b.owner_id
            JOIN pets p ON p.id = b.pet_id
            JOIN partner_profiles pp ON pp.partner_id = b.partner_id
            JOIN partner_services s ON s.id = b.service_id
        """
    }

    @Transactional(readOnly = true)
    fun profile(partnerId: UUID): PartnerProfileDto {
        val services = services(partnerId, activeOnly = true)
        return jdbc.sql("SELECT * FROM partner_profiles WHERE partner_id = :id").param("id", partnerId).query { rs, _ ->
            PartnerProfileDto(
                partnerId, rs.getString("business_name"), rs.getString("category"), rs.getString("description"), rs.getString("website"),
                rs.getString("phone"), rs.getString("address"), rs.getString("opening_hours"), rs.getString("cover_url"),
                rs.getBoolean("verified"), rs.getObject("place_id", UUID::class.java), services,
            )
        }.optional().orElseThrow { NotFoundException("Partner not found") }
    }

    @Transactional(readOnly = true)
    fun services(partnerId: UUID, activeOnly: Boolean): List<PartnerServiceDto> =
        jdbc.sql("SELECT * FROM partner_services WHERE partner_id = :id AND (active OR NOT :activeOnly) ORDER BY name, id")
            .param("id", partnerId).param("activeOnly", activeOnly).query { rs, _ -> service(rs) }.list()

    @Transactional
    fun createService(partnerId: UUID, request: PartnerServiceRequest): PartnerServiceDto {
        requireProfile(partnerId)
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO partner_services (id, partner_id, name, description, duration_minutes, price_cents, active, created_at, updated_at)
            VALUES (:id, :partner, :name, :description, :duration, :price, :active, now(), now())
            """,
        ).param("id", id).param("partner", partnerId).bindService(request).update()
        return service(partnerId, id)
    }

    @Transactional
    fun updateService(partnerId: UUID, serviceId: UUID, request: PartnerServiceRequest): PartnerServiceDto {
        val updated = jdbc.sql(
            """
            UPDATE partner_services SET name = :name, description = :description, duration_minutes = :duration, price_cents = :price,
                   active = :active, updated_at = now()
            WHERE id = :id AND partner_id = :partner
            """,
        ).param("id", serviceId).param("partner", partnerId).bindService(request).update()
        if (updated == 0) throw NotFoundException("Service not found")
        return service(partnerId, serviceId)
    }

    @Transactional
    fun book(ownerId: UUID, request: CreateBookingRequest, source: BookingSource = BookingSource.APP, aiActionId: UUID? = null): BookingDto {
        val petId = request.petId!!
        val startsAt = request.startsAt!!
        petAccess.requireOwnedActivePet(ownerId, petId)
        if (!startsAt.isAfter(Instant.now().plus(Duration.ofMinutes(30)))) throw BadRequestException("Bookings must start at least 30 minutes from now")
        data class Svc(val partnerId: UUID, val duration: Int)
        val svc = jdbc.sql(
            """
            SELECT s.partner_id, s.duration_minutes FROM partner_services s
            JOIN owners o ON o.id = s.partner_id AND o.status = 'ACTIVE'
            WHERE s.id = :id AND s.active FOR UPDATE OF s
            """,
        ).param("id", request.serviceId).query { rs, _ -> Svc(rs.getObject(1, UUID::class.java), rs.getInt(2)) }
            .optional().orElseThrow { NotFoundException("Service not found") }
        if (svc.partnerId == ownerId) throw BadRequestException("Partners cannot book their own services")
        val endsAt = startsAt.plus(Duration.ofMinutes(svc.duration.toLong()))
        val overlapping = jdbc.sql(
            """
            SELECT COUNT(*) FROM bookings b JOIN partner_services s ON s.id = b.service_id
            WHERE b.service_id = :service AND b.status IN ('REQUESTED', 'CONFIRMED')
              AND b.starts_at < :ends AND b.starts_at + make_interval(mins => s.duration_minutes) > :starts
            """,
        ).param("service", request.serviceId).param("starts", Timestamp.from(startsAt)).param("ends", Timestamp.from(endsAt))
            .query(Long::class.java).single()
        if (overlapping > 0) throw ConflictException("This time slot is no longer available")
        val campaignId = request.campaignId?.takeIf { campaign ->
            jdbc.sql("SELECT COUNT(*) FROM partner_campaigns WHERE id = :id AND partner_id = :partner")
                .param("id", campaign).param("partner", svc.partnerId).query(Long::class.java).single() > 0
        }
        val effectiveSource = if (source == BookingSource.APP && campaignId != null) BookingSource.AD else source
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO bookings (id, owner_id, pet_id, partner_id, service_id, starts_at, status, source, campaign_id, ai_action_id, notes, created_at, updated_at)
            VALUES (:id, :owner, :pet, :partner, :service, :starts, 'REQUESTED', :source, :campaign, :action, :notes, now(), now())
            """,
        ).param("id", id).param("owner", ownerId).param("pet", petId).param("partner", svc.partnerId).param("service", request.serviceId)
            .param("starts", Timestamp.from(startsAt)).param("source", effectiveSource.name).param("campaign", campaignId)
            .param("action", aiActionId).param("notes", request.notes?.trim()?.ifEmpty { null }).update()
        if (campaignId != null) {
            val viewer = MessageDigest.getInstance("SHA-256").digest(ownerId.toString().toByteArray()).joinToString("") { "%02x".format(it) }
            jdbc.sql(
                "INSERT INTO campaign_events (id, campaign_id, type, placement, viewer_hash, occurred_at) VALUES (:id, :campaign, 'BOOKING', :placement, :viewer, now())",
            ).param("id", UUID.randomUUID()).param("campaign", campaignId)
                .param("placement", if (source == BookingSource.AI) "AI_RECOMMENDATION" else "FEED_CARD").param("viewer", viewer).update()
        }
        log.info("booking requested booking={} partner={} source={} campaign={}", id, svc.partnerId, effectiveSource, campaignId)
        return booking(id)
    }

    @Transactional(readOnly = true)
    fun ownerBookings(ownerId: UUID, page: PageRequest): PageResponse<BookingDto> = PageResponse.of(
        jdbc.sql("$BOOKING_SELECT WHERE b.owner_id = :owner ORDER BY b.starts_at DESC, b.id LIMIT :limit OFFSET :offset")
            .param("owner", ownerId).param("limit", page.limit).param("offset", page.offset).query { rs, _ -> booking(rs) }.list(),
        page.page, page.size,
    )

    @Transactional(readOnly = true)
    fun partnerBookings(partnerId: UUID, page: PageRequest): PageResponse<BookingDto> = PageResponse.of(
        jdbc.sql("$BOOKING_SELECT WHERE b.partner_id = :partner ORDER BY b.starts_at ASC, b.id LIMIT :limit OFFSET :offset")
            .param("partner", partnerId).param("limit", page.limit).param("offset", page.offset).query { rs, _ -> booking(rs) }.list(),
        page.page, page.size,
    )

    @Transactional
    fun cancel(actorId: UUID, bookingId: UUID): BookingDto {
        val current = lock(bookingId)
        if (actorId != current.ownerId && actorId != current.partnerId) throw ForbiddenException("Not your booking")
        if (current.status !in setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED)) throw ConflictException("Booking can no longer be cancelled")
        return transition(bookingId, BookingStatus.CANCELLED)
    }

    @Transactional
    fun partnerTransition(partnerId: UUID, bookingId: UUID, target: BookingStatus): BookingDto {
        val current = lock(bookingId)
        if (current.partnerId != partnerId) throw ForbiddenException("Not your booking")
        val allowed = when (target) {
            BookingStatus.CONFIRMED -> current.status == BookingStatus.REQUESTED
            BookingStatus.COMPLETED -> current.status == BookingStatus.CONFIRMED
            else -> false
        }
        if (!allowed) throw ConflictException("Cannot move booking from ${current.status} to $target")
        return transition(bookingId, target)
    }

    private fun transition(bookingId: UUID, target: BookingStatus): BookingDto {
        jdbc.sql("UPDATE bookings SET status = :status, updated_at = now(), version = version + 1 WHERE id = :id")
            .param("status", target.name).param("id", bookingId).update()
        return booking(bookingId)
    }

    private fun lock(bookingId: UUID): BookingDto {
        jdbc.sql("SELECT id FROM bookings WHERE id = :id FOR UPDATE").param("id", bookingId).query(UUID::class.java)
            .optional().orElseThrow { NotFoundException("Booking not found") }
        return booking(bookingId)
    }

    private fun booking(id: UUID): BookingDto =
        jdbc.sql("$BOOKING_SELECT WHERE b.id = :id").param("id", id).query { rs, _ -> booking(rs) }.single()

    private fun booking(rs: ResultSet): BookingDto {
        val starts = rs.getTimestamp("starts_at").toInstant()
        return BookingDto(
            rs.getObject("id", UUID::class.java), rs.getObject("owner_id", UUID::class.java), rs.getString("owner_name"),
            rs.getObject("pet_id", UUID::class.java), rs.getString("pet_name"), rs.getObject("partner_id", UUID::class.java),
            rs.getString("partner_name"), rs.getObject("service_id", UUID::class.java), rs.getString("service_name"),
            rs.getLong("price_cents"), rs.getString("currency"), starts, starts.plus(Duration.ofMinutes(rs.getLong("duration_minutes"))),
            BookingStatus.valueOf(rs.getString("status")), BookingSource.valueOf(rs.getString("source")),
            rs.getObject("campaign_id", UUID::class.java), rs.getString("notes"),
        )
    }

    private fun service(partnerId: UUID, id: UUID): PartnerServiceDto =
        jdbc.sql("SELECT * FROM partner_services WHERE id = :id AND partner_id = :partner").param("id", id).param("partner", partnerId)
            .query { rs, _ -> service(rs) }.single()

    private fun service(rs: ResultSet) = PartnerServiceDto(
        rs.getObject("id", UUID::class.java), rs.getObject("partner_id", UUID::class.java), rs.getString("name"), rs.getString("description"),
        rs.getInt("duration_minutes"), rs.getLong("price_cents"), rs.getString("currency"), rs.getBoolean("active"),
    )

    private fun requireProfile(partnerId: UUID) {
        if (jdbc.sql("SELECT COUNT(*) FROM partner_profiles WHERE partner_id = :id").param("id", partnerId).query(Long::class.java).single() == 0L) {
            throw NotFoundException("Partner profile not found")
        }
    }

    private fun JdbcClient.StatementSpec.bindService(request: PartnerServiceRequest) = this
        .param("name", request.name!!.trim()).param("description", request.description?.trim()?.ifEmpty { null })
        .param("duration", request.durationMinutes).param("price", request.priceCents).param("active", request.active ?: true)
}
