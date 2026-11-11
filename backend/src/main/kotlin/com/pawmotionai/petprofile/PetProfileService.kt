package com.pawmotionai.petprofile

import com.pawmotionai.engagement.EngagementService
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

/** Pet profile aggregate: identity, gamification, stats, badge shelf, weekly activity chart and tab feeds. */
@Service
class PetProfileService(private val jdbc: JdbcClient, private val engagement: EngagementService) {

    @Transactional(readOnly = true)
    fun profile(viewerId: UUID, petId: UUID): PetProfileDto {
        val pet = pet(petId)
        val owner = jdbc.sql("SELECT id, display_name, neighborhood, city FROM owners WHERE id = (SELECT owner_id FROM pets WHERE id = :pet)")
            .param("pet", petId).query { rs, _ ->
                ProfileOwnerDto(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getString(3), rs.getString(4))
            }.single()
        val stats = jdbc.sql(
            """
            SELECT (SELECT COUNT(*) FROM pet_follows WHERE pet_id = :pet) AS followers,
                   (SELECT COUNT(*) FROM activities WHERE pet_id = :pet) AS adventures,
                   (SELECT COUNT(*) FROM posts WHERE pet_id = :pet) AS posts
            """,
        ).param("pet", petId).query { rs, _ ->
            ProfileStatsDto(rs.getLong("followers"), rs.getLong("adventures"), rs.getLong("posts") + rs.getLong("adventures"))
        }.single()
        val following = jdbc.sql("SELECT COUNT(*) FROM pet_follows WHERE follower_owner_id = :viewer AND pet_id = :pet")
            .param("viewer", viewerId).param("pet", petId).query(Long::class.java).single() > 0
        return PetProfileDto(
            pet, owner, engagement.engagement(petId), stats,
            engagement.badges(petId).sortedByDescending { it.awardedAt ?: Instant.EPOCH }.take(6),
            weeklyActivity(petId), following, owner.id == viewerId,
        )
    }

    @Transactional(readOnly = true)
    fun posts(petId: UUID, page: PageRequest): PageResponse<PetPostDto> {
        pet(petId)
        val rows = jdbc.sql(
            """
            SELECT p.id, f.id AS feed_item_id, p.caption, p.place_name, f.published_at,
                   (SELECT COUNT(*) FROM reactions r WHERE r.feed_item_id = f.id) AS reactions,
                   (SELECT COUNT(*) FROM comments c WHERE c.feed_item_id = f.id) AS comments
            FROM posts p JOIN feed_items f ON f.type = 'POST' AND f.ref_id = p.id AND f.deleted_at IS NULL
            WHERE p.pet_id = :pet ORDER BY f.published_at DESC LIMIT :limit OFFSET :offset
            """,
        ).param("pet", petId).param("limit", page.limit).param("offset", page.offset).query { rs, _ ->
            PetPostDto(rs.getObject("id", UUID::class.java), rs.getObject("feed_item_id", UUID::class.java), rs.getString("caption"),
                rs.getString("place_name"), postMedia(rs.getObject("id", UUID::class.java)), rs.getTimestamp("published_at").toInstant(),
                rs.getLong("reactions"), rs.getLong("comments"))
        }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    @Transactional(readOnly = true)
    fun adventures(petId: UUID, page: PageRequest): PageResponse<AdventureDto> {
        pet(petId)
        val rows = jdbc.sql(
            """
            SELECT a.id, a.type, a.started_at, a.duration_seconds, a.distance_meters, a.notes, f.id AS feed_item_id
            FROM activities a LEFT JOIN feed_items f ON f.type = 'ACTIVITY' AND f.ref_id = a.id AND f.deleted_at IS NULL
            WHERE a.pet_id = :pet ORDER BY a.started_at DESC LIMIT :limit OFFSET :offset
            """,
        ).param("pet", petId).param("limit", page.limit).param("offset", page.offset).query { rs, _ ->
            AdventureDto(rs.getObject("id", UUID::class.java), rs.getObject("feed_item_id", UUID::class.java), rs.getString("type"),
                rs.getTimestamp("started_at").toInstant(), rs.getLong("duration_seconds"),
                (rs.getObject("distance_meters") as Number?)?.toDouble(), rs.getString("notes"))
        }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    /** Mutual friends: pets the owner follows whose owners also follow this pet. */
    @Transactional(readOnly = true)
    fun friends(viewerId: UUID, petId: UUID, page: PageRequest): PageResponse<PetFriendDto> {
        val pet = pet(petId)
        val ownerId = jdbc.sql("SELECT owner_id FROM pets WHERE id = :pet").param("pet", petId).query(UUID::class.java).single()
        val rows = jdbc.sql(
            """
            SELECT q.id, q.name, lower(q.species) AS species, q.breed, q.avatar_url, o.display_name, GREATEST(f1.created_at, f2.created_at) AS since
            FROM pet_follows f1
            JOIN pets q ON q.id = f1.pet_id AND q.status = 'ACTIVE'
            JOIN owners o ON o.id = q.owner_id AND o.status = 'ACTIVE'
            JOIN pet_follows f2 ON f2.follower_owner_id = q.owner_id AND f2.pet_id = :pet
            WHERE f1.follower_owner_id = :owner
            ORDER BY since DESC LIMIT :limit OFFSET :offset
            """,
        ).param("pet", petId).param("owner", ownerId).param("limit", page.limit).param("offset", page.offset)
            .query { rs, _ ->
                PetFriendDto(rs.getObject("id", UUID::class.java), rs.getString("name"), rs.getString("species"), rs.getString("breed"),
                    rs.getString("avatar_url"), rs.getString("display_name"), rs.getTimestamp("since").toInstant())
            }.list()
        return PageResponse.of(rows, page.page, page.size)
    }

    /** Minutes of activity per weekday for the current ISO week (Mon–Sun), used by the profile chart. */
    private fun weeklyActivity(petId: UUID): List<WeeklyBarDto> {
        val weekStart = LocalDate.now(ZoneOffset.UTC).with(java.time.DayOfWeek.MONDAY)
        val rows = jdbc.sql(
            """
            SELECT started_at::date AS day, COALESCE(SUM(duration_seconds), 0) AS seconds
            FROM activities WHERE pet_id = :pet AND started_at::date BETWEEN :start AND :start + 6
            GROUP BY started_at::date
            """,
        ).param("pet", petId).param("start", weekStart).query { rs, _ ->
            rs.getDate(1).toLocalDate() to rs.getLong(2)
        }.list().toMap()
        return (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            WeeklyBarDto(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(), (rows[day] ?: 0L) / 60)
        }
    }

    private fun postMedia(postId: UUID): List<String> = jdbc.sql(
        "SELECT m.id FROM post_media pm JOIN media_assets m ON m.id = pm.media_id WHERE pm.post_id = :post ORDER BY pm.position",
    ).param("post", postId).query { rs, _ -> "/api/media/${rs.getObject(1, UUID::class.java)}" }.list()

    private fun pet(petId: UUID): ProfilePetDto = jdbc.sql(
        "SELECT id, name, lower(species) AS species, breed, birth_date, size, avatar_url, cover_url, bio, activity_preference FROM pets WHERE id = :id AND status = 'ACTIVE'",
    ).param("id", petId).query { rs, _ -> profilePet(rs) }.optional().orElseThrow { NotFoundException("Pet not found") }

    private fun profilePet(rs: ResultSet) = ProfilePetDto(
        rs.getObject("id", UUID::class.java), rs.getString("name"), rs.getString("species"), rs.getString("breed"),
        rs.getDate("birth_date")?.toLocalDate(), rs.getString("size"), rs.getString("avatar_url"), rs.getString("cover_url"),
        rs.getString("bio"), rs.getString("activity_preference"),
    )
}
