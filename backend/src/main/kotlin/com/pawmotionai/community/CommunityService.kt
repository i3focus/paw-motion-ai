package com.pawmotionai.community

import com.pawmotionai.feed.application.FeedService
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.MediaUrls
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/** Communities by breed, neighborhood and cause, with member-created events and RSVPs. */
@Service
class CommunityService(private val jdbc: JdbcClient) {

    private val log = LoggerFactory.getLogger(CommunityService::class.java)

    companion object {
        private const val POINT = "ST_SetSRID(ST_MakePoint(CAST(:lng AS float8), CAST(:lat AS float8)), 4326)::geography"

        private const val EVENT_SELECT = """
            SELECT e.id, e.community_id, c.name AS community_name, e.title, e.description, e.location_name,
                   ST_Y(e.location::geometry) AS lat, ST_X(e.location::geometry) AS lng,
                   e.starts_at, e.ends_at, e.capacity,
                   (SELECT COUNT(*) FROM event_rsvps r WHERE r.event_id = e.id AND r.status = 'GOING') AS going,
                   (SELECT COUNT(*) FROM event_rsvps r WHERE r.event_id = e.id AND r.status = 'INTERESTED') AS interested,
                   (SELECT r.status FROM event_rsvps r WHERE r.event_id = e.id AND r.owner_id = :viewer) AS my_rsvp
            FROM community_events e JOIN communities c ON c.id = e.community_id
        """
    }

    @Transactional(readOnly = true)
    fun communities(viewerId: UUID, kind: CommunityKind?, query: String?, page: PageRequest): PageResponse<CommunityDto> {
        val rows = jdbc.sql(
            """
            SELECT c.*, (SELECT COUNT(*) FROM community_members m WHERE m.community_id = c.id) AS members,
                   (SELECT COUNT(*) FROM community_events e WHERE e.community_id = c.id AND e.starts_at > now()) AS upcoming,
                   m.role AS my_role
            FROM communities c LEFT JOIN community_members m ON m.community_id = c.id AND m.owner_id = :viewer
            WHERE (CAST(:kind AS text) IS NULL OR c.kind = :kind)
              AND (CAST(:q AS text) IS NULL OR c.name ILIKE '%' || CAST(:q AS text) || '%' OR c.description ILIKE '%' || CAST(:q AS text) || '%')
            ORDER BY members DESC, c.name LIMIT :limit OFFSET :offset
            """,
        ).param("viewer", viewerId).param("kind", kind?.name).param("q", query?.trim()?.take(80)?.ifEmpty { null })
            .param("limit", page.limit).param("offset", page.offset).query { rs, _ -> community(rs) }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    @Transactional
    fun create(viewerId: UUID, request: CreateCommunityRequest): CommunityDto {
        if ((request.latitude == null) != (request.longitude == null)) throw BadRequestException("latitude and longitude must be provided together")
        val id = UUID.randomUUID()
        val slug = uniqueSlug(request.name!!)
        jdbc.sql(
            """
            INSERT INTO communities (id, slug, name, kind, description, cover_url, location, created_by, created_at)
            VALUES (:id, :slug, :name, :kind, :description, :cover,
                    CASE WHEN CAST(:lat AS float8) IS NULL THEN NULL ELSE $POINT END, :creator, now())
            """,
        ).param("id", id).param("slug", slug).param("name", request.name.trim()).param("kind", request.kind!!.name)
            .param("description", request.description?.trim()?.ifEmpty { null }).param("cover", request.coverUrl?.trim()?.ifEmpty { null })
            .param("lat", request.latitude).param("lng", request.longitude).param("creator", viewerId).update()
        jdbc.sql("INSERT INTO community_members (community_id, owner_id, role, joined_at) VALUES (:c, :owner, 'MODERATOR', now())")
            .param("c", id).param("owner", viewerId).update()
        log.info("community created community={} by={}", id, viewerId)
        return community(id, viewerId)
    }

    @Transactional(readOnly = true)
    fun community(id: UUID, viewerId: UUID): CommunityDto =
        jdbc.sql(
            """
            SELECT c.*, (SELECT COUNT(*) FROM community_members m WHERE m.community_id = c.id) AS members,
                   (SELECT COUNT(*) FROM community_events e WHERE e.community_id = c.id AND e.starts_at > now()) AS upcoming,
                   m.role AS my_role
            FROM communities c LEFT JOIN community_members m ON m.community_id = c.id AND m.owner_id = :viewer WHERE c.id = :id
            """,
        ).param("viewer", viewerId).param("id", id).query { rs, _ -> community(rs) }.optional()
            .orElseThrow { NotFoundException("Community not found") }

    @Transactional
    fun join(viewerId: UUID, id: UUID, join: Boolean): CommunityDto {
        requireCommunity(id)
        if (join) {
            jdbc.sql("INSERT INTO community_members (community_id, owner_id, role, joined_at) VALUES (:c, :owner, 'MEMBER', now()) ON CONFLICT DO NOTHING")
                .param("c", id).param("owner", viewerId).update()
        } else {
            val role = jdbc.sql("SELECT role FROM community_members WHERE community_id = :c AND owner_id = :owner")
                .param("c", id).param("owner", viewerId).query(String::class.java).optional()
                .orElseThrow { NotFoundException("Not a member of this community") }
            if (role == "MODERATOR" && jdbc.sql("SELECT COUNT(*) FROM community_members WHERE community_id = :c AND role = 'MODERATOR'")
                    .param("c", id).query(Long::class.java).single() == 1L) {
                throw ConflictException("The last moderator cannot leave the community")
            }
            jdbc.sql("DELETE FROM community_members WHERE community_id = :c AND owner_id = :owner").param("c", id).param("owner", viewerId).update()
        }
        return community(id, viewerId)
    }

    @Transactional(readOnly = true)
    fun events(viewerId: UUID, communityId: UUID, page: PageRequest, upcomingOnly: Boolean): PageResponse<CommunityEventDto> {
        requireCommunity(communityId)
        val rows = jdbc.sql(
            """
            $EVENT_SELECT WHERE e.community_id = :community
              AND (:all OR e.starts_at > now() - interval '1 day')
            ORDER BY e.starts_at LIMIT :limit OFFSET :offset
            """,
        ).param("viewer", viewerId).param("community", communityId).param("all", !upcomingOnly)
            .param("limit", page.limit).param("offset", page.offset).query { rs, _ -> event(rs) }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    @Transactional
    fun createEvent(viewerId: UUID, communityId: UUID, request: CreateEventRequest): CommunityEventDto {
        requireMember(communityId, viewerId)
        if ((request.latitude == null) != (request.longitude == null)) throw BadRequestException("latitude and longitude must be provided together")
        if (request.startsAt!!.isBefore(java.time.Instant.now().minusSeconds(300))) throw BadRequestException("startsAt must be in the future")
        if (request.endsAt != null && request.endsAt.isBefore(request.startsAt)) throw BadRequestException("endsAt must be after startsAt")
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO community_events (id, community_id, title, description, location, location_name, starts_at, ends_at, capacity, created_by, created_at)
            VALUES (:id, :community, :title, :description,
                    CASE WHEN CAST(:lat AS float8) IS NULL THEN NULL ELSE $POINT END,
                    :locationName, :starts, :ends, :capacity, :creator, now())
            """,
        ).param("id", id).param("community", communityId).param("title", request.title!!.trim())
            .param("description", request.description?.trim()?.ifEmpty { null })
            .param("lat", request.latitude).param("lng", request.longitude)
            .param("locationName", request.locationName?.trim()?.ifEmpty { null })
            .param("starts", Timestamp.from(request.startsAt)).param("ends", request.endsAt?.let(Timestamp::from))
            .param("capacity", request.capacity).param("creator", viewerId).update()
        log.info("community event created event={} community={} by={}", id, communityId, viewerId)
        return jdbc.sql("$EVENT_SELECT WHERE e.id = :id").param("id", id).param("viewer", viewerId)
            .query { rs, _ -> event(rs) }.single()
    }

    @Transactional
    fun rsvp(viewerId: UUID, eventId: UUID, status: RsvpStatus?): CommunityEventDto {
        val event = eventOrThrow(eventId)
        if (status == null) {
            jdbc.sql("DELETE FROM event_rsvps WHERE event_id = :event AND owner_id = :owner").param("event", eventId).param("owner", viewerId).update()
        } else {
            if (status == RsvpStatus.GOING && event.capacity != null) {
                val going = jdbc.sql("SELECT COUNT(*) FROM event_rsvps WHERE event_id = :event AND status = 'GOING'")
                    .param("event", eventId).query(Long::class.java).single()
                val already = jdbc.sql("SELECT COUNT(*) FROM event_rsvps WHERE event_id = :event AND owner_id = :owner AND status = 'GOING'")
                    .param("event", eventId).param("owner", viewerId).query(Long::class.java).single() > 0
                if (!already && going >= event.capacity) throw ConflictException("This event is at full capacity")
            }
            jdbc.sql(
                """
                INSERT INTO event_rsvps (event_id, owner_id, status, created_at) VALUES (:event, :owner, :status, now())
                ON CONFLICT (event_id, owner_id) DO UPDATE SET status = EXCLUDED.status
                """,
            ).param("event", eventId).param("owner", viewerId).param("status", status.name).update()
        }
        return jdbc.sql("$EVENT_SELECT WHERE e.id = :id").param("id", eventId).param("viewer", viewerId)
            .query { rs, _ -> event(rs) }.single()
    }

    /** Discover: highlighted partner placements, nearby communities and upcoming events. */
    @Transactional(readOnly = true)
    fun discover(viewerId: UUID, lat: Double?, lng: Double?): DiscoverDto {
        FeedService.validateCoordinates(lat, lng)
        val sponsored = if (lat == null) emptyList() else jdbc.sql(
            """
            SELECT c.id, c.partner_id, c.title, c.description, c.cta, c.cta_type, c.cta_url, c.service_id, c.media_asset_id,
                   COALESCE(pp.business_name, o.display_name) AS partner_name, COALESCE(pp.verified, FALSE) AS verified
            FROM partner_campaigns c
            JOIN owners o ON o.id = c.partner_id AND o.status = 'ACTIVE'
            LEFT JOIN partner_profiles pp ON pp.partner_id = c.partner_id
            WHERE c.status = 'ACTIVE' AND c.starts_at <= now() AND c.ends_at > now()
              AND 'DISCOVER_HIGHLIGHT' = ANY(c.formats)
              AND (c.target_location IS NULL OR ST_DWithin(c.target_location, $POINT, COALESCE(c.target_radius_m, 50000)))
            ORDER BY c.created_at DESC LIMIT 6
            """,
        ).param("lat", lat).param("lng", lng).query { rs, _ ->
            DiscoverPartnerDto(rs.getObject("id", UUID::class.java), rs.getObject("partner_id", UUID::class.java), rs.getString("partner_name"),
                rs.getBoolean("verified"), rs.getString("title"), rs.getString("description"), rs.getString("cta"), rs.getString("cta_type"),
                rs.getString("cta_url"), rs.getObject("service_id", UUID::class.java), rs.getObject("media_asset_id", UUID::class.java)?.let(MediaUrls::of))
        }.list()
        val communities = jdbc.sql(
            """
            SELECT c.*, (SELECT COUNT(*) FROM community_members m WHERE m.community_id = c.id) AS members,
                   (SELECT COUNT(*) FROM community_events e WHERE e.community_id = c.id AND e.starts_at > now()) AS upcoming,
                   m.role AS my_role,
                   CASE WHEN CAST(:lat AS float8) IS NULL OR c.location IS NULL THEN NULL ELSE ST_Distance(c.location, $POINT) END AS distance
            FROM communities c LEFT JOIN community_members m ON m.community_id = c.id AND m.owner_id = :viewer
            ORDER BY distance ASC NULLS LAST, members DESC LIMIT 8
            """,
        ).param("viewer", viewerId).param("lat", lat).param("lng", lng).query { rs, _ -> community(rs) }.list()
        val events = jdbc.sql(
            """
            $EVENT_SELECT WHERE e.starts_at > now()
              AND (CAST(:lat AS float8) IS NULL OR e.location IS NULL OR ST_DWithin(e.location, $POINT, 50000))
            ORDER BY e.starts_at LIMIT 8
            """,
        ).param("viewer", viewerId).param("lat", lat).param("lng", lng).query { rs, _ -> event(rs) }.list()
        return DiscoverDto(sponsored, communities, events)
    }

    private fun requireCommunity(id: UUID) {
        if (jdbc.sql("SELECT COUNT(*) FROM communities WHERE id = :id").param("id", id).query(Long::class.java).single() == 0L) {
            throw NotFoundException("Community not found")
        }
    }

    private fun requireMember(communityId: UUID, viewerId: UUID) {
        requireCommunity(communityId)
        val member = jdbc.sql("SELECT COUNT(*) FROM community_members WHERE community_id = :c AND owner_id = :owner")
            .param("c", communityId).param("owner", viewerId).query(Long::class.java).single() > 0
        if (!member) throw ForbiddenException("Join the community to create events")
    }

    private fun eventOrThrow(eventId: UUID): CommunityEventDto =
        jdbc.sql("$EVENT_SELECT WHERE e.id = :id").param("id", eventId).param("viewer", UUID(0, 0))
            .query { rs, _ -> event(rs) }.optional().orElseThrow { NotFoundException("Event not found") }

    private fun uniqueSlug(name: String): String {
        val base = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "community" }.take(60)
        var slug = base
        var suffix = 2
        while (jdbc.sql("SELECT COUNT(*) FROM communities WHERE slug = :slug").param("slug", slug).query(Long::class.java).single() > 0) {
            slug = "$base-${suffix++}"
        }
        return slug
    }

    private fun community(rs: ResultSet) = CommunityDto(
        rs.getObject("id", UUID::class.java), rs.getString("slug"), rs.getString("name"), CommunityKind.valueOf(rs.getString("kind")),
        rs.getString("description"), rs.getString("cover_url"), rs.getLong("members"), rs.getString("my_role") != null, rs.getString("my_role"),
        rs.getLong("upcoming"),
    )

    private fun event(rs: ResultSet) = CommunityEventDto(
        rs.getObject("id", UUID::class.java), rs.getObject("community_id", UUID::class.java), rs.getString("community_name"),
        rs.getString("title"), rs.getString("description"), rs.getString("location_name"),
        (rs.getObject("lat") as Number?)?.toDouble(), (rs.getObject("lng") as Number?)?.toDouble(),
        rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at")?.toInstant(),
        (rs.getObject("capacity") as Number?)?.toInt(), rs.getLong("going"), rs.getLong("interested"),
        rs.getString("my_rsvp")?.let(RsvpStatus::valueOf),
    )
}
