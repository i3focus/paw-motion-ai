package com.pawmotionai.content

import com.pawmotionai.engagement.EngagementService
import com.pawmotionai.media.MediaService
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.MediaUrls
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PetAccess
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class ContentService(private val jdbc: JdbcClient, private val petAccess: PetAccess, private val media: MediaService, private val engagement: EngagementService) {

    private val log = LoggerFactory.getLogger(ContentService::class.java)

    companion object {
        val STORY_TTL: Duration = Duration.ofHours(24)
        private const val POINT = "CASE WHEN CAST(:lat AS float8) IS NULL THEN NULL ELSE ST_SetSRID(ST_MakePoint(CAST(:lng AS float8), CAST(:lat AS float8)), 4326)::geography END"
    }

    @Transactional
    fun createPost(ownerId: UUID, request: CreatePostRequest): PostCreatedDto {
        val petId = request.petId!!
        val mediaIds = request.mediaIds!!.distinct()
        val tagged = request.taggedPetIds.orEmpty().distinct().filter { it != petId }
        if ((request.latitude == null) != (request.longitude == null)) throw BadRequestException("latitude and longitude must be provided together")
        petAccess.requireOwnedActivePet(ownerId, petId)
        petAccess.requireActivePets(tagged)
        if (!media.ownsAll(ownerId, mediaIds)) throw ForbiddenException("Media must be uploaded by the post author")

        val postId = UUID.randomUUID()
        val feedItemId = UUID.randomUUID()
        val now = Timestamp.from(Instant.now())
        jdbc.sql(
            """
            INSERT INTO posts (id, owner_id, pet_id, caption, place_name, location, created_at, updated_at)
            VALUES (:id, :owner, :pet, :caption, :place, $POINT, :now, :now)
            """,
        ).param("id", postId).param("owner", ownerId).param("pet", petId)
            .param("caption", request.caption?.trim()?.ifEmpty { null }).param("place", request.placeName?.trim()?.ifEmpty { null })
            .param("lat", request.latitude).param("lng", request.longitude).param("now", now).update()
        mediaIds.forEachIndexed { index, mediaId ->
            jdbc.sql("INSERT INTO post_media (post_id, media_id, position) VALUES (:post, :media, :pos)")
                .param("post", postId).param("media", mediaId).param("pos", index).update()
        }
        tagged.forEach { tag ->
            jdbc.sql("INSERT INTO post_pet_tags (post_id, pet_id) VALUES (:post, :pet)").param("post", postId).param("pet", tag).update()
        }
        jdbc.sql(
            """
            INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, location, published_at)
            SELECT :id, 'POST', p.id, p.owner_id, p.pet_id, p.location, p.created_at FROM posts p WHERE p.id = :post
            """,
        ).param("id", feedItemId).param("post", postId).update()
        engagement.recordPost(postId, ownerId, petId)
        log.info("post created post={} owner={} media={} tags={}", postId, ownerId, mediaIds.size, tagged.size)
        return PostCreatedDto(postId, feedItemId)
    }

    @Transactional
    fun deletePost(ownerId: UUID, postId: UUID) {
        val author = jdbc.sql("SELECT owner_id FROM posts WHERE id = :id").param("id", postId).query(UUID::class.java)
            .optional().orElseThrow { NotFoundException("Post not found") }
        if (author != ownerId) throw ForbiddenException("Only the author can delete this post")
        jdbc.sql("UPDATE feed_items SET deleted_at = now() WHERE type = 'POST' AND ref_id = :id").param("id", postId).update()
        jdbc.sql("DELETE FROM posts WHERE id = :id").param("id", postId).update()
    }

    @Transactional
    fun createStory(ownerId: UUID, request: CreateStoryRequest): StoryDto {
        petAccess.requireOwnedActivePet(ownerId, request.petId!!)
        if (!media.ownsAll(ownerId, listOf(request.mediaId!!))) throw ForbiddenException("Media must be uploaded by the story author")
        val id = UUID.randomUUID()
        val now = Instant.now()
        jdbc.sql(
            """
            INSERT INTO stories (id, owner_id, pet_id, media_id, caption, created_at, expires_at)
            VALUES (:id, :owner, :pet, :media, :caption, :now, :expires)
            """,
        ).param("id", id).param("owner", ownerId).param("pet", request.petId).param("media", request.mediaId)
            .param("caption", request.caption?.trim()?.ifEmpty { null }).param("now", Timestamp.from(now))
            .param("expires", Timestamp.from(now.plus(STORY_TTL))).update()
        return stories(ownerId).flatMap { it.stories }.first { it.id == id }
    }

    /** Active stories from the viewer's pets, followed pets and the viewer's neighborhood; unseen groups first. */
    @Transactional(readOnly = true)
    fun stories(viewerId: UUID): List<StoryGroupDto> {
        data class Row(val group: StoryGroupDto, val story: StoryDto)
        val rows = jdbc.sql(
            """
            SELECT s.id, s.pet_id, p.name AS pet_name, p.avatar_url, s.owner_id, s.media_id, m.content_type, s.caption,
                   s.created_at, s.expires_at,
                   EXISTS (SELECT 1 FROM story_views v WHERE v.story_id = s.id AND v.viewer_id = :viewer) AS viewed
            FROM stories s
            JOIN pets p ON p.id = s.pet_id AND p.status = 'ACTIVE'
            JOIN media_assets m ON m.id = s.media_id
            JOIN owners o ON o.id = s.owner_id AND o.status = 'ACTIVE'
            JOIN owners me ON me.id = :viewer
            WHERE s.expires_at > now()
              AND (s.owner_id = :viewer
                   OR EXISTS (SELECT 1 FROM pet_follows f WHERE f.follower_owner_id = :viewer AND f.pet_id = s.pet_id)
                   OR (me.neighborhood IS NOT NULL AND lower(o.neighborhood) = lower(me.neighborhood))
                   OR (me.location IS NOT NULL AND o.location IS NOT NULL AND ST_DWithin(me.location, o.location, 10000)))
            ORDER BY s.created_at ASC
            LIMIT 500
            """,
        ).param("viewer", viewerId).query { rs, _ ->
            val ownerId = rs.getObject("owner_id", UUID::class.java)
            Row(
                StoryGroupDto(rs.getObject("pet_id", UUID::class.java), rs.getString("pet_name"), rs.getString("avatar_url"),
                    ownerId, ownerId == viewerId, false, emptyList()),
                StoryDto(rs.getObject("id", UUID::class.java), MediaUrls.of(rs.getObject("media_id", UUID::class.java)),
                    rs.getString("content_type"), rs.getString("caption"), rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("viewed")),
            )
        }.list()
        return rows.groupBy { it.group.petId }.values.map { group ->
            val stories = group.map { it.story }
            group.first().group.copy(stories = stories, allViewed = stories.all { it.viewed })
        }.sortedWith(compareByDescending<StoryGroupDto> { it.own }.thenBy { it.allViewed }.thenByDescending { g -> g.stories.maxOf { it.createdAt } })
    }

    @Transactional
    fun markStoryViewed(viewerId: UUID, storyId: UUID) {
        val updated = jdbc.sql(
            """
            INSERT INTO story_views (story_id, viewer_id, viewed_at)
            SELECT id, :viewer, now() FROM stories WHERE id = :id AND expires_at > now()
            ON CONFLICT DO NOTHING
            """,
        ).param("viewer", viewerId).param("id", storyId).update()
        if (updated == 0 && jdbc.sql("SELECT COUNT(*) FROM stories WHERE id = :id AND expires_at > now()").param("id", storyId)
                .query(Long::class.java).single() == 0L) {
            throw NotFoundException("Story not found")
        }
    }

    @Transactional
    fun follow(viewerId: UUID, petId: UUID, following: Boolean): FollowStateDto {
        petAccess.requireActivePets(listOf(petId))
        if (following) {
            jdbc.sql("INSERT INTO pet_follows (follower_owner_id, pet_id, created_at) VALUES (:viewer, :pet, now()) ON CONFLICT DO NOTHING")
                .param("viewer", viewerId).param("pet", petId).update()
        } else {
            jdbc.sql("DELETE FROM pet_follows WHERE follower_owner_id = :viewer AND pet_id = :pet").param("viewer", viewerId).param("pet", petId).update()
        }
        val followers = jdbc.sql("SELECT COUNT(*) FROM pet_follows WHERE pet_id = :pet").param("pet", petId).query(Long::class.java).single()
        return FollowStateDto(petId, following, followers)
    }
}
