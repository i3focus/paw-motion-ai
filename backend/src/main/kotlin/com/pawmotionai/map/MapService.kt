package com.pawmotionai.map

import com.pawmotionai.engagement.EngagementService
import com.pawmotionai.feed.application.FeedService
import com.pawmotionai.media.MediaService
import com.pawmotionai.notification.NotificationType
import com.pawmotionai.notification.Notifier
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.MediaUrls
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.PetAccess
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class MapService(
    private val jdbc: JdbcClient,
    private val petAccess: PetAccess,
    private val media: MediaService,
    private val engagement: EngagementService,
    private val notifier: Notifier,
) {

    private val log = LoggerFactory.getLogger(MapService::class.java)

    companion object {
        const val MAX_RADIUS_M = 50_000
        private const val POINT = "ST_SetSRID(ST_MakePoint(CAST(:lng AS float8), CAST(:lat AS float8)), 4326)::geography"
        private const val PLACE_SELECT = """
            SELECT pl.id, pl.name, pl.category, ST_Y(pl.location::geometry) AS lat, ST_X(pl.location::geometry) AS lng,
                   ST_Distance(pl.location, $POINT) AS distance, pl.address, pl.description, pl.rating, pl.rating_count,
                   pl.partner_id, COALESCE(pp.verified, FALSE) AS verified,
                   (SELECT COUNT(*) FROM check_ins c WHERE c.place_id = pl.id AND c.created_at > now() - interval '1 day') AS check_ins_today
            FROM places pl LEFT JOIN partner_profiles pp ON pp.partner_id = pl.partner_id
        """
    }

    @Transactional(readOnly = true)
    fun nearbyPlaces(lat: Double, lng: Double, radius: Int?, categories: Set<PlaceCategory>, query: String?, page: PageRequest): PageResponse<PlaceDto> {
        FeedService.validateCoordinates(lat, lng)
        val rows = jdbc.sql(
            """
            $PLACE_SELECT
            WHERE ST_DWithin(pl.location, $POINT, :radius)
              AND (:allCategories OR pl.category IN (:categories))
              AND (CAST(:q AS text) IS NULL OR pl.name ILIKE '%' || CAST(:q AS text) || '%')
            ORDER BY distance ASC, pl.id
            LIMIT :limit OFFSET :offset
            """,
        ).param("lat", lat).param("lng", lng).param("radius", (radius ?: 5000).coerceIn(100, MAX_RADIUS_M))
            .param("allCategories", categories.isEmpty())
            .param("categories", categories.ifEmpty { setOf(PlaceCategory.OTHER) }.map { it.name })
            .param("q", query?.trim()?.take(80)?.ifEmpty { null })
            .param("limit", page.limit).param("offset", page.offset)
            .query { rs, _ -> place(rs) }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    @Transactional(readOnly = true)
    fun place(placeId: UUID, lat: Double?, lng: Double?): PlaceDto {
        FeedService.validateCoordinates(lat, lng)
        return jdbc.sql("$PLACE_SELECT WHERE pl.id = :id").param("id", placeId).param("lat", lat).param("lng", lng)
            .query { rs, _ -> place(rs) }.optional().orElseThrow { NotFoundException("Place not found") }
    }

    @Transactional
    fun checkIn(ownerId: UUID, placeId: UUID, petId: UUID): CheckInDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        if (jdbc.sql("SELECT COUNT(*) FROM places WHERE id = :id").param("id", placeId).query(Long::class.java).single() == 0L) {
            throw NotFoundException("Place not found")
        }
        val recent = jdbc.sql("SELECT COUNT(*) FROM check_ins WHERE place_id = :place AND pet_id = :pet AND created_at > now() - interval '1 hour'")
            .param("place", placeId).param("pet", petId).query(Long::class.java).single()
        if (recent > 0) throw ConflictException("Already checked in here within the last hour")
        val id = UUID.randomUUID()
        val now = Instant.now()
        jdbc.sql("INSERT INTO check_ins (id, place_id, owner_id, pet_id, created_at) VALUES (:id, :place, :owner, :pet, :now)")
            .param("id", id).param("place", placeId).param("owner", ownerId).param("pet", petId).param("now", Timestamp.from(now)).update()
        engagement.recordCheckIn(id, ownerId, petId)
        return CheckInDto(id, placeId, petId, now)
    }

    @Transactional
    fun createLostPetAlert(ownerId: UUID, request: CreateLostPetAlertRequest): LostPetAlertDto {
        petAccess.requireOwnedActivePet(ownerId, request.petId!!)
        request.photoMediaId?.let { if (!media.ownsAll(ownerId, listOf(it))) throw ForbiddenException("Photo must be uploaded by the owner") }
        if (request.lastSeenAt!!.isAfter(Instant.now().plusSeconds(300))) throw BadRequestException("lastSeenAt cannot be in the future")
        val active = jdbc.sql("SELECT COUNT(*) FROM lost_pet_alerts WHERE pet_id = :pet AND status = 'ACTIVE'").param("pet", request.petId)
            .query(Long::class.java).single()
        if (active > 0) throw ConflictException("This pet already has an active alert")
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO lost_pet_alerts (id, pet_id, owner_id, photo_media_id, description, contact_phone, last_seen_location,
                                         last_seen_at, radius_m, status, created_at)
            VALUES (:id, :pet, :owner, :photo, :description, :phone, $POINT, :seen, :radius, 'ACTIVE', now())
            """,
        ).param("id", id).param("pet", request.petId).param("owner", ownerId).param("photo", request.photoMediaId)
            .param("description", request.description!!.trim()).param("phone", request.contactPhone?.trim()?.ifEmpty { null })
            .param("lat", request.latitude).param("lng", request.longitude).param("seen", Timestamp.from(request.lastSeenAt))
            .param("radius", request.radiusMeters ?: 3000).update()
        val petName = jdbc.sql("SELECT name FROM pets WHERE id = :id").param("id", request.petId).query(String::class.java).single()
        notifier.notifyNearby(ownerId, request.latitude!!, request.longitude!!, request.radiusMeters ?: 3000, NotificationType.LOST_PET,
            "Lost pet near you: $petName", request.description!!.trim().take(120), mapOf("alertId" to id.toString()))
        log.info("lost pet alert created alert={} pet={}", id, request.petId)
        return alert(id, null, null)
    }

    @Transactional(readOnly = true)
    fun nearbyLostPets(lat: Double, lng: Double): List<LostPetAlertDto> {
        FeedService.validateCoordinates(lat, lng)
        return jdbc.sql(
            "${alertSelect()} WHERE a.status = 'ACTIVE' AND ST_DWithin(a.last_seen_location, $POINT, GREATEST(a.radius_m, 5000)) ORDER BY distance LIMIT 50",
        ).param("lat", lat).param("lng", lng).query { rs, _ -> alertRow(rs) }.list()
    }

    @Transactional
    fun reportSighting(reporterId: UUID, alertId: UUID, request: SightingRequest) {
        val inserted = jdbc.sql(
            """
            INSERT INTO lost_pet_sightings (id, alert_id, reporter_owner_id, location, seen_at, note, created_at)
            SELECT :id, a.id, :reporter, $POINT, :seen, :note, now() FROM lost_pet_alerts a WHERE a.id = :alert AND a.status = 'ACTIVE'
            """,
        ).param("id", UUID.randomUUID()).param("reporter", reporterId).param("lat", request.latitude).param("lng", request.longitude)
            .param("seen", Timestamp.from(request.seenAt)).param("note", request.note?.trim()?.ifEmpty { null }).param("alert", alertId).update()
        if (inserted == 0) throw NotFoundException("Active alert not found")
    }

    @Transactional
    fun resolveAlert(ownerId: UUID, alertId: UUID, found: Boolean): LostPetAlertDto {
        val owner = jdbc.sql("SELECT owner_id FROM lost_pet_alerts WHERE id = :id AND status = 'ACTIVE'").param("id", alertId)
            .query(UUID::class.java).optional().orElseThrow { NotFoundException("Active alert not found") }
        if (owner != ownerId) throw ForbiddenException("Only the owner can resolve this alert")
        jdbc.sql("UPDATE lost_pet_alerts SET status = :status, resolved_at = now() WHERE id = :id")
            .param("status", if (found) "FOUND" else "CANCELLED").param("id", alertId).update()
        return alert(alertId, null, null)
    }

    @Transactional
    fun createPlaydate(ownerId: UUID, request: CreatePlaydateRequest): PlaydateDto {
        val petId = request.petId!!
        petAccess.requireOwnedActivePet(ownerId, petId)
        if ((request.latitude == null) != (request.longitude == null)) throw BadRequestException("latitude and longitude must be provided together")
        val startsAt = when (request.mode!!) {
            PlaydateMode.NOW -> Instant.now()
            PlaydateMode.SCHEDULED -> request.startsAt?.takeIf { it.isAfter(Instant.now()) }
                ?: throw BadRequestException("Scheduled playdates need a future startsAt")
        }
        val invitees = request.inviteePetIds.orEmpty().distinct().filter { it != petId }
        petAccess.requireActivePets(invitees)
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO playdates (id, organizer_owner_id, organizer_pet_id, place_id, location, location_name, mode, starts_at, message, status, created_at)
            VALUES (:id, :owner, :pet, :place,
                    CASE WHEN CAST(:lat AS float8) IS NULL THEN (SELECT location FROM places WHERE id = :place) ELSE $POINT END,
                    COALESCE(:locationName, (SELECT name FROM places WHERE id = :place)), :mode, :starts, :message, 'OPEN', now())
            """,
        ).param("id", id).param("owner", ownerId).param("pet", petId).param("place", request.placeId)
            .param("lat", request.latitude).param("lng", request.longitude).param("locationName", request.locationName?.trim()?.ifEmpty { null })
            .param("mode", request.mode.name).param("starts", Timestamp.from(startsAt)).param("message", request.message?.trim()?.ifEmpty { null })
            .update()
        invitees.forEach { invitee ->
            jdbc.sql(
                """
                INSERT INTO playdate_invites (id, playdate_id, invitee_pet_id, invitee_owner_id, status, created_at)
                SELECT :id, :playdate, p.id, p.owner_id, 'PENDING', now() FROM pets p WHERE p.id = :pet
                """,
            ).param("id", UUID.randomUUID()).param("playdate", id).param("pet", invitee).update()
            jdbc.sql("SELECT owner_id FROM pets WHERE id = :pet").param("pet", invitee).query(UUID::class.java).optional()
                .ifPresent { inviteeOwner ->
                    notifier.notify(inviteeOwner, NotificationType.PLAYDATE, "Playdate invite",
                        request.message?.take(120) ?: "You were invited to a playdate.", mapOf("playdateId" to id.toString()))
                }
        }
        engagement.recordPlaydate(id, ownerId, petId)
        return playdate(id)
    }

    @Transactional
    fun respondToInvite(ownerId: UUID, playdateId: UUID, petId: UUID, response: InviteResponse): PlaydateDto {
        val updated = jdbc.sql(
            """
            UPDATE playdate_invites SET status = :status, responded_at = now()
            WHERE playdate_id = :playdate AND invitee_pet_id = :pet AND invitee_owner_id = :owner
              AND EXISTS (SELECT 1 FROM playdates d WHERE d.id = :playdate AND d.status = 'OPEN')
            """,
        ).param("status", response.name).param("playdate", playdateId).param("pet", petId).param("owner", ownerId).update()
        if (updated == 0) throw NotFoundException("Invite not found")
        if (response == InviteResponse.ACCEPTED) {
            engagement.recordPlaydate(playdateId, ownerId, petId)
            jdbc.sql("SELECT organizer_owner_id FROM playdates WHERE id = :id").param("id", playdateId).query(UUID::class.java).optional()
                .ifPresent { organizer ->
                    val petName = jdbc.sql("SELECT name FROM pets WHERE id = :pet").param("pet", petId).query(String::class.java).single()
                    notifier.notify(organizer, NotificationType.PLAYDATE, "Playdate accepted", "$petName accepted your playdate invite.",
                        mapOf("playdateId" to playdateId.toString()))
                }
        }
        return playdate(playdateId)
    }

    @Transactional(readOnly = true)
    fun myPlaydates(ownerId: UUID): List<PlaydateDto> = jdbc.sql(
        """
        SELECT d.id FROM playdates d
        WHERE d.status = 'OPEN' AND d.starts_at > now() - interval '3 hours'
          AND (d.organizer_owner_id = :owner OR EXISTS (SELECT 1 FROM playdate_invites i WHERE i.playdate_id = d.id AND i.invitee_owner_id = :owner))
        ORDER BY d.starts_at LIMIT 50
        """,
    ).param("owner", ownerId).query(UUID::class.java).list().map { playdate(it!!) }

    @Transactional(readOnly = true)
    fun nearbyPets(viewerId: UUID, lat: Double, lng: Double, radius: Int?): List<NearbyPetDto> {
        FeedService.validateCoordinates(lat, lng)
        return jdbc.sql(
            """
            SELECT p.id, p.name, lower(p.species) AS species, p.breed, p.avatar_url, o.display_name, ST_Distance(o.location, $POINT) AS distance
            FROM pets p JOIN owners o ON o.id = p.owner_id
            WHERE p.status = 'ACTIVE' AND o.status = 'ACTIVE' AND o.id <> :viewer AND o.role = 'OWNER'
              AND o.location IS NOT NULL AND ST_DWithin(o.location, $POINT, :radius)
            ORDER BY distance, p.id LIMIT 50
            """,
        ).param("lat", lat).param("lng", lng).param("viewer", viewerId).param("radius", (radius ?: 2000).coerceIn(100, 10_000))
            .query { rs, _ ->
                NearbyPetDto(rs.getObject("id", UUID::class.java), rs.getString("name"), rs.getString("species"), rs.getString("breed"),
                    rs.getString("avatar_url"), rs.getString("display_name"), (rs.getDouble("distance") / 100).toLong() * 100.0)
            }.list()
    }

    private fun playdate(id: UUID): PlaydateDto {
        val invites = jdbc.sql(
            "SELECT i.invitee_pet_id, p.name, i.status FROM playdate_invites i JOIN pets p ON p.id = i.invitee_pet_id WHERE i.playdate_id = :id ORDER BY p.name",
        ).param("id", id).query { rs, _ -> PlaydateInviteDto(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getString(3)) }.list()
        return jdbc.sql(
            """
            SELECT d.*, p.name AS pet_name, ST_Y(d.location::geometry) AS lat, ST_X(d.location::geometry) AS lng
            FROM playdates d JOIN pets p ON p.id = d.organizer_pet_id WHERE d.id = :id
            """,
        ).param("id", id).query { rs, _ ->
            PlaydateDto(
                id, rs.getObject("organizer_owner_id", UUID::class.java), rs.getObject("organizer_pet_id", UUID::class.java),
                rs.getString("pet_name"), PlaydateMode.valueOf(rs.getString("mode")), rs.getTimestamp("starts_at").toInstant(),
                rs.getObject("place_id", UUID::class.java), rs.getString("location_name"), rs.getObject("lat") as Double?,
                rs.getObject("lng") as Double?, rs.getString("message"), rs.getString("status"), invites,
            )
        }.single()
    }

    private fun alertSelect() = """
        SELECT a.id, a.pet_id, p.name AS pet_name, lower(p.species) AS pet_species, a.owner_id, a.description, a.contact_phone,
               a.photo_media_id, ST_Y(a.last_seen_location::geometry) AS lat, ST_X(a.last_seen_location::geometry) AS lng,
               a.last_seen_at, a.radius_m, a.status,
               CASE WHEN CAST(:lat AS float8) IS NULL THEN NULL ELSE ST_Distance(a.last_seen_location, $POINT) END AS distance,
               (SELECT COUNT(*) FROM lost_pet_sightings s WHERE s.alert_id = a.id) AS sightings
        FROM lost_pet_alerts a JOIN pets p ON p.id = a.pet_id
    """

    private fun alert(id: UUID, lat: Double?, lng: Double?): LostPetAlertDto =
        jdbc.sql("${alertSelect()} WHERE a.id = :id").param("id", id).param("lat", lat).param("lng", lng)
            .query { rs, _ -> alertRow(rs) }.single()

    private fun alertRow(rs: ResultSet) = LostPetAlertDto(
        rs.getObject("id", UUID::class.java), rs.getObject("pet_id", UUID::class.java), rs.getString("pet_name"), rs.getString("pet_species"),
        rs.getObject("owner_id", UUID::class.java), rs.getString("description"), rs.getString("contact_phone"),
        rs.getObject("photo_media_id", UUID::class.java)?.let(MediaUrls::of), rs.getDouble("lat"), rs.getDouble("lng"),
        rs.getTimestamp("last_seen_at").toInstant(), rs.getInt("radius_m"), rs.getString("status"),
        (rs.getObject("distance") as Number?)?.toDouble(), rs.getLong("sightings"),
    )

    private fun place(rs: ResultSet) = PlaceDto(
        rs.getObject("id", UUID::class.java), rs.getString("name"), PlaceCategory.valueOf(rs.getString("category")),
        rs.getDouble("lat"), rs.getDouble("lng"), (rs.getObject("distance") as Number?)?.toDouble(), rs.getString("address"),
        rs.getString("description"), (rs.getObject("rating") as Number?)?.toDouble(), rs.getInt("rating_count"),
        rs.getObject("partner_id", UUID::class.java), rs.getBoolean("verified"), rs.getLong("check_ins_today"),
    )
}
