package com.pawmotionai.feed.adapter

import com.pawmotionai.feed.api.AchievementContentDto
import com.pawmotionai.feed.api.ActivityContentDto
import com.pawmotionai.feed.api.AdEventType
import com.pawmotionai.feed.api.AdPlacement
import com.pawmotionai.feed.api.CommentDto
import com.pawmotionai.feed.api.MediaDto
import com.pawmotionai.feed.api.PostContentDto
import com.pawmotionai.feed.api.ReactionSummaryDto
import com.pawmotionai.feed.api.ReactionType
import com.pawmotionai.feed.api.SponsoredContentDto
import com.pawmotionai.feed.domain.AdCandidate
import com.pawmotionai.feed.domain.ViewerProfile
import com.pawmotionai.shared.MediaUrls
import com.pawmotionai.shared.PageRequest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Viewer(
    val ownerId: UUID,
    val lat: Double?,
    val lng: Double?,
    val neighborhood: String?,
    val profile: ViewerProfile,
) {
    val hasLocation: Boolean get() = lat != null && lng != null
}

data class FeedRow(
    val id: UUID,
    val type: String,
    val refId: UUID,
    val ownerId: UUID,
    val ownerName: String,
    val neighborhood: String?,
    val petId: UUID?,
    val petName: String?,
    val petAvatarUrl: String?,
    val petSpecies: String?,
    val publishedAt: Instant,
    val own: Boolean,
    val followed: Boolean,
    val reactionCount: Long,
    val commentCount: Long,
)

data class SponsoredRow(val content: SponsoredContentDto, val candidate: AdCandidate)

@Repository
class JdbcFeedRepository(private val jdbc: JdbcClient) {

    companion object {
        const val NEARBY_RADIUS_M = 10_000
        const val CANDIDATE_LIMIT = 500
        private const val VIEWER_POINT =
            "ST_SetSRID(ST_MakePoint(CAST(:lng AS float8), CAST(:lat AS float8)), 4326)::geography"
    }

    fun viewer(ownerId: UUID, lat: Double?, lng: Double?): Viewer {
        data class OwnerRow(val neighborhood: String?, val lat: Double?, val lng: Double?)
        val owner = jdbc.sql(
            """
            SELECT neighborhood, ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lng
            FROM owners WHERE id = :id
            """,
        ).param("id", ownerId)
            .query { rs, _ -> OwnerRow(rs.getString("neighborhood"), rs.nullableDouble("lat"), rs.nullableDouble("lng")) }
            .single()
        data class PetRow(val species: String, val breed: String?, val preference: String?)
        val pets = jdbc.sql("SELECT species, breed, activity_preference FROM pets WHERE owner_id = :id AND status = 'ACTIVE'")
            .param("id", ownerId)
            .query { rs, _ -> PetRow(rs.getString(1), rs.getString(2), rs.getString(3)) }
            .list()
        val interests = pets.flatMap { pet -> pet.preference.orEmpty().lowercase().split(Regex("[^\\p{L}]+")).filter { it.length > 2 } }
        val useRequest = lat != null && lng != null
        return Viewer(
            ownerId,
            if (useRequest) lat else owner.lat,
            if (useRequest) lng else owner.lng,
            owner.neighborhood,
            ViewerProfile(pets.map { it.species }.toSet(), pets.mapNotNull { it.breed }.toSet(), interests.toSet()),
        )
    }

    fun candidates(viewer: Viewer, since: Instant): List<FeedRow> = jdbc.sql(
        """
        SELECT f.id, f.type, f.ref_id, f.author_owner_id, f.author_pet_id, f.published_at,
               o.display_name AS owner_name, o.neighborhood, p.name AS pet_name, p.avatar_url AS pet_avatar_url,
               lower(p.species) AS pet_species,
               (f.author_owner_id = :viewer) AS own,
               EXISTS (SELECT 1 FROM pet_follows pf WHERE pf.follower_owner_id = :viewer AND pf.pet_id = f.author_pet_id) AS followed,
               (SELECT COUNT(*) FROM reactions r WHERE r.feed_item_id = f.id) AS reaction_count,
               (SELECT COUNT(*) FROM comments c WHERE c.feed_item_id = f.id) AS comment_count
        FROM feed_items f
        JOIN owners o ON o.id = f.author_owner_id AND o.status = 'ACTIVE'
        LEFT JOIN pets p ON p.id = f.author_pet_id
        WHERE f.deleted_at IS NULL AND f.published_at >= :since
          AND (f.author_owner_id = :viewer
               OR EXISTS (SELECT 1 FROM pet_follows pf WHERE pf.follower_owner_id = :viewer AND pf.pet_id = f.author_pet_id)
               OR (:hasLocation AND f.location IS NOT NULL AND ST_DWithin(f.location, $VIEWER_POINT, :radius))
               OR (CAST(:neighborhood AS text) IS NOT NULL AND lower(o.neighborhood) = lower(CAST(:neighborhood AS text)))
               OR (NOT :hasLocation AND CAST(:neighborhood AS text) IS NULL))
        ORDER BY f.published_at DESC
        LIMIT :limit
        """,
    )
        .param("viewer", viewer.ownerId)
        .param("since", java.sql.Timestamp.from(since))
        .param("hasLocation", viewer.hasLocation)
        .param("lat", viewer.lat)
        .param("lng", viewer.lng)
        .param("radius", NEARBY_RADIUS_M)
        .param("neighborhood", viewer.neighborhood)
        .param("limit", CANDIDATE_LIMIT)
        .query { rs, _ ->
            FeedRow(
                rs.uuid("id"), rs.getString("type"), rs.uuid("ref_id"), rs.uuid("author_owner_id"), rs.getString("owner_name"),
                rs.getString("neighborhood"), rs.nullableUuid("author_pet_id"), rs.getString("pet_name"),
                rs.getString("pet_avatar_url"), rs.getString("pet_species"), rs.instant("published_at"),
                rs.getBoolean("own"), rs.getBoolean("followed"), rs.getLong("reaction_count"), rs.getLong("comment_count"),
            )
        }
        .list()

    fun activities(ids: Collection<UUID>): Map<UUID, ActivityContentDto> = if (ids.isEmpty()) emptyMap() else jdbc.sql(
        "SELECT id, lower(type) AS type, started_at, duration_seconds, distance_meters, notes FROM activities WHERE id IN (:ids)",
    ).param("ids", ids).query { rs, _ ->
        ActivityContentDto(rs.uuid("id"), rs.getString("type"), rs.instant("started_at"), rs.getLong("duration_seconds"),
            rs.nullableDouble("distance_meters"), rs.getString("notes"))
    }.list().associateBy { it.activityId }

    fun posts(ids: Collection<UUID>): Map<UUID, PostContentDto> {
        if (ids.isEmpty()) return emptyMap()
        val media = jdbc.sql(
            """
            SELECT pm.post_id, m.id, m.content_type FROM post_media pm JOIN media_assets m ON m.id = pm.media_id
            WHERE pm.post_id IN (:ids) ORDER BY pm.position
            """,
        ).param("ids", ids).query { rs, _ -> rs.uuid("post_id") to MediaDto(rs.uuid("id"), MediaUrls.of(rs.uuid("id")), rs.getString("content_type")) }
            .list().groupBy({ it.first }, { it.second })
        val tags = jdbc.sql("SELECT post_id, pet_id FROM post_pet_tags WHERE post_id IN (:ids)").param("ids", ids)
            .query { rs, _ -> rs.uuid("post_id") to rs.uuid("pet_id") }.list().groupBy({ it.first }, { it.second })
        return jdbc.sql("SELECT id, caption, place_name FROM posts WHERE id IN (:ids)").param("ids", ids).query { rs, _ ->
            val id = rs.uuid("id")
            PostContentDto(id, rs.getString("caption"), rs.getString("place_name"), media[id].orEmpty(), tags[id].orEmpty())
        }.list().associateBy { it.postId }
    }

    fun achievements(ids: Collection<UUID>): Map<UUID, AchievementContentDto> = if (ids.isEmpty()) emptyMap() else jdbc.sql(
        "SELECT pb.id, b.code, b.name, b.description, b.emoji FROM pet_badges pb JOIN badges b ON b.code = pb.badge_code WHERE pb.id IN (:ids)",
    ).param("ids", ids).query { rs, _ ->
        rs.uuid("id") to AchievementContentDto(rs.getString("code"), rs.getString("name"), rs.getString("description"), rs.getString("emoji"))
    }.list().toMap()

    fun reactionSummaries(itemIds: Collection<UUID>, viewerId: UUID): Map<UUID, ReactionSummaryDto> {
        if (itemIds.isEmpty()) return emptyMap()
        val counts = jdbc.sql("SELECT feed_item_id, type, COUNT(*) AS total FROM reactions WHERE feed_item_id IN (:ids) GROUP BY feed_item_id, type")
            .param("ids", itemIds)
            .query { rs, _ -> Triple(rs.uuid("feed_item_id"), ReactionType.valueOf(rs.getString("type")), rs.getLong("total")) }
            .list().groupBy({ it.first }, { it.second to it.third })
        val mine = jdbc.sql("SELECT feed_item_id, type FROM reactions WHERE feed_item_id IN (:ids) AND owner_id = :viewer")
            .param("ids", itemIds).param("viewer", viewerId)
            .query { rs, _ -> rs.uuid("feed_item_id") to ReactionType.valueOf(rs.getString("type")) }.list().toMap()
        return itemIds.associateWith { id ->
            val byType = counts[id].orEmpty().toMap()
            ReactionSummaryDto(ReactionType.entries.associateWith { byType[it] ?: 0L }, mine[id])
        }
    }

    fun feedItemExists(itemId: UUID): Boolean =
        jdbc.sql("SELECT COUNT(*) FROM feed_items WHERE id = :id AND deleted_at IS NULL").param("id", itemId)
            .query(Long::class.java).single() > 0

    fun upsertReaction(itemId: UUID, ownerId: UUID, type: ReactionType) {
        jdbc.sql(
            """
            INSERT INTO reactions (id, feed_item_id, owner_id, type, created_at, updated_at)
            VALUES (:id, :item, :owner, :type, now(), now())
            ON CONFLICT (feed_item_id, owner_id) DO UPDATE SET type = EXCLUDED.type, legacy_type = NULL, updated_at = now()
            """,
        ).param("id", UUID.randomUUID()).param("item", itemId).param("owner", ownerId).param("type", type.name).update()
    }

    fun deleteReaction(itemId: UUID, ownerId: UUID) {
        jdbc.sql("DELETE FROM reactions WHERE feed_item_id = :item AND owner_id = :owner")
            .param("item", itemId).param("owner", ownerId).update()
    }

    fun comments(itemId: UUID, page: PageRequest): List<CommentDto> = jdbc.sql(
        """
        SELECT c.id, c.feed_item_id, c.owner_id, o.display_name, o.avatar_url, c.body, c.created_at
        FROM comments c JOIN owners o ON o.id = c.owner_id
        WHERE c.feed_item_id = :item
        ORDER BY c.created_at ASC, c.id ASC
        LIMIT :limit OFFSET :offset
        """,
    ).param("item", itemId).param("limit", page.limit).param("offset", page.offset).query { rs, _ -> comment(rs) }.list()

    fun addComment(itemId: UUID, ownerId: UUID, body: String): CommentDto {
        val id = UUID.randomUUID()
        jdbc.sql("INSERT INTO comments (id, feed_item_id, owner_id, body, created_at) VALUES (:id, :item, :owner, :body, now())")
            .param("id", id).param("item", itemId).param("owner", ownerId).param("body", body).update()
        return jdbc.sql(
            """
            SELECT c.id, c.feed_item_id, c.owner_id, o.display_name, o.avatar_url, c.body, c.created_at
            FROM comments c JOIN owners o ON o.id = c.owner_id WHERE c.id = :id
            """,
        ).param("id", id).query { rs, _ -> comment(rs) }.single()
    }

    fun feedItemAuthor(itemId: UUID): UUID? =
        jdbc.sql("SELECT author_owner_id FROM feed_items WHERE id = :id").param("id", itemId)
            .query { rs, _ -> rs.uuid("author_owner_id") }.optional().orElse(null)

    fun sponsoredCandidates(viewer: Viewer, placement: AdPlacement): List<SponsoredRow> = jdbc.sql(
        """
        SELECT c.id, c.partner_id, c.title, c.description, c.cta, c.cta_type, c.cta_url, c.service_id, c.media_asset_id,
               c.target_species, c.target_breeds, c.target_interests, c.target_radius_m,
               COALESCE(pp.business_name, NULLIF(o.business_name, ''), o.display_name) AS partner_name,
               COALESCE(pp.verified, FALSE) AS verified,
               CASE WHEN c.target_location IS NULL OR NOT :hasLocation THEN NULL
                    ELSE ST_Distance(c.target_location, $VIEWER_POINT) END AS distance
        FROM partner_campaigns c
        JOIN owners o ON o.id = c.partner_id AND o.status = 'ACTIVE'
        LEFT JOIN partner_profiles pp ON pp.partner_id = c.partner_id
        WHERE c.status = 'ACTIVE' AND c.starts_at <= now() AND c.ends_at > now()
          AND CAST(:placement AS varchar) = ANY(c.formats)
          AND (c.target_location IS NULL OR :hasLocation)
        ORDER BY c.id
        LIMIT 100
        """,
    )
        .param("hasLocation", viewer.hasLocation)
        .param("lat", viewer.lat)
        .param("lng", viewer.lng)
        .param("placement", placement.name)
        .query { rs, _ ->
            val id = rs.uuid("id")
            SponsoredRow(
                SponsoredContentDto(
                    campaignId = id,
                    partnerId = rs.uuid("partner_id"),
                    partnerName = rs.getString("partner_name"),
                    verified = rs.getBoolean("verified"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    ctaLabel = rs.getString("cta"),
                    ctaType = rs.getString("cta_type"),
                    ctaUrl = rs.getString("cta_url"),
                    serviceId = rs.nullableUuid("service_id"),
                    mediaUrl = rs.nullableUuid("media_asset_id")?.let(MediaUrls::of),
                ),
                AdCandidate(
                    id, rs.nullableDouble("distance"), rs.getObject("target_radius_m") as Int?,
                    rs.stringArray("target_species"), rs.stringArray("target_breeds"), rs.stringArray("target_interests"),
                ),
            )
        }
        .list()

    fun campaignServable(campaignId: UUID): Boolean =
        jdbc.sql("SELECT COUNT(*) FROM partner_campaigns WHERE id = :id AND status = 'ACTIVE' AND starts_at <= now() AND ends_at > now()")
            .param("id", campaignId).query(Long::class.java).single() > 0

    /** Records an ad event; impressions from the same viewer and placement are counted once per 30 minutes. */
    fun recordAdEvent(campaignId: UUID, type: AdEventType, placement: AdPlacement, viewerHash: String): Boolean = jdbc.sql(
        """
        INSERT INTO campaign_events (id, campaign_id, type, placement, viewer_hash, occurred_at)
        SELECT :id, :campaign, :type, :placement, :viewer, now()
        WHERE :type <> 'IMPRESSION' OR NOT EXISTS (
            SELECT 1 FROM campaign_events e
            WHERE e.campaign_id = :campaign AND e.type = 'IMPRESSION' AND e.placement = :placement
              AND e.viewer_hash = :viewer AND e.occurred_at > now() - interval '30 minutes')
        """,
    ).param("id", UUID.randomUUID()).param("campaign", campaignId).param("type", type.name)
        .param("placement", placement.name).param("viewer", viewerHash).update() > 0

    private fun comment(rs: ResultSet) = CommentDto(
        rs.uuid("id"), rs.uuid("feed_item_id"), rs.uuid("owner_id"), rs.getString("display_name"),
        rs.getString("avatar_url"), rs.getString("body"), rs.instant("created_at"),
    )
}

internal fun ResultSet.uuid(column: String): UUID = getObject(column, UUID::class.java)
internal fun ResultSet.nullableUuid(column: String): UUID? = getObject(column, UUID::class.java)
internal fun ResultSet.instant(column: String): Instant = getTimestamp(column).toInstant()
internal fun ResultSet.nullableDouble(column: String): Double? = (getObject(column) as Number?)?.toDouble()
internal fun ResultSet.stringArray(column: String): Set<String> =
    (getArray(column)?.array as Array<*>?)?.filterIsInstance<String>()?.toSet().orEmpty()
