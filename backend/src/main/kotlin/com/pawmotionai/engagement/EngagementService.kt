package com.pawmotionai.engagement

import com.pawmotionai.notification.Notifier
import com.pawmotionai.notification.NotificationType
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PetAccess
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Gamification: XP ledger, levels, streaks, badge awarding and weekly challenges (refactor plan §4.2 `engagement`).
 * XP events are idempotent per (pet, source_key); awarding a badge publishes an ACHIEVEMENT feed item and notifies
 * the owner.
 */
@Service
class EngagementService(private val jdbc: JdbcClient, private val petAccess: PetAccess, private val notifier: Notifier) {

    private val log = LoggerFactory.getLogger(EngagementService::class.java)

    companion object {
        const val XP_POST = 15
        const val XP_CHECK_IN = 10
        const val XP_PLAYDATE = 20

        fun xpForActivity(distanceMeters: Double?): Int =
            10 + minOf(50, ((distanceMeters ?: 0.0) / 1000.0).toInt() * 5)

        fun levelFor(xp: Long): Int = floor(sqrt(xp / 25.0)).toInt() + 1

        /** XP required to reach [level]; mirrors [levelFor]. */
        fun xpForLevel(level: Int): Long = ((level - 1L) * (level - 1L)) * 25L

        fun levelTitle(level: Int): String = when {
            level >= 20 -> "Legend"
            level >= 15 -> "Adventurer"
            level >= 10 -> "Explorer"
            level >= 6 -> "Scout"
            level >= 3 -> "Walker"
            else -> "Newcomer"
        }
    }

    // ---- XP recording (invoked by activity/content/map modules) ----

    @Transactional(propagation = Propagation.REQUIRED)
    fun recordActivity(activityId: UUID, ownerId: UUID, petId: UUID, distanceMeters: Double?, occurredAt: Instant) {
        awardXp(petId, ownerId, "ACTIVITY", "activity:$activityId", xpForActivity(distanceMeters), occurredAt)
        evaluateBadges(petId)
        evaluateChallenges(petId)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    fun recordPost(postId: UUID, ownerId: UUID, petId: UUID) {
        awardXp(petId, ownerId, "POST", "post:$postId", XP_POST, Instant.now())
        evaluateBadges(petId)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    fun recordCheckIn(checkInId: UUID, ownerId: UUID, petId: UUID) {
        awardXp(petId, ownerId, "CHECK_IN", "checkin:$checkInId", XP_CHECK_IN, Instant.now())
        evaluateBadges(petId)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    fun recordPlaydate(playdateId: UUID, ownerId: UUID, petId: UUID) {
        awardXp(petId, ownerId, "PLAYDATE", "playdate:$playdateId:$petId", XP_PLAYDATE, Instant.now())
        evaluateBadges(petId)
    }

    private fun awardXp(petId: UUID, ownerId: UUID, sourceType: String, sourceKey: String, points: Int, occurredAt: Instant) {
        val inserted = jdbc.sql(
            """
            INSERT INTO xp_events (id, pet_id, owner_id, source_type, source_key, points, occurred_at)
            VALUES (:id, :pet, :owner, :type, :key, :points, :at) ON CONFLICT (pet_id, source_key) DO NOTHING
            """,
        ).param("id", UUID.randomUUID()).param("pet", petId).param("owner", ownerId).param("type", sourceType)
            .param("key", sourceKey).param("points", points).param("at", Timestamp.from(occurredAt)).update()
        if (inserted > 0) log.info("xp awarded pet={} source={} points={}", petId, sourceType, points)
    }

    // ---- badges ----

    /** Awards newly-earned badges; publishes ACHIEVEMENT feed items and notifies the pet owner. */
    fun evaluateBadges(petId: UUID) {
        val stats = badgeStats(petId)
        val newly = jdbc.sql(
            """
            INSERT INTO pet_badges (pet_id, badge_code, awarded_at)
            SELECT :pet, b.code, now() FROM badges b
            WHERE (b.criteria = 'ACTIVITY_COUNT' AND :activities >= b.threshold)
               OR (b.criteria = 'DISTANCE_METERS' AND :distance >= b.threshold)
               OR (b.criteria = 'STREAK_DAYS' AND :streak >= b.threshold)
               OR (b.criteria = 'POST_COUNT' AND :posts >= b.threshold)
               OR (b.criteria = 'CHECK_IN_COUNT' AND :checkins >= b.threshold)
               OR (b.criteria = 'PLAYDATE_COUNT' AND :playdates >= b.threshold)
            ON CONFLICT (pet_id, badge_code) DO NOTHING
            RETURNING id, badge_code
            """,
        ).param("pet", petId).param("activities", stats.activities).param("distance", stats.distanceMeters)
            .param("streak", stats.streakDays).param("posts", stats.posts).param("checkins", stats.checkIns)
            .param("playdates", stats.playdates)
            .query { rs, _ -> rs.getObject("id", UUID::class.java) to rs.getString("badge_code") }.list()
        if (newly.isEmpty()) return
        val (ownerId, petName) = jdbc.sql("SELECT o.id, p.name FROM pets p JOIN owners o ON o.id = p.owner_id WHERE p.id = :pet")
            .param("pet", petId).query { rs, _ -> rs.getObject(1, UUID::class.java) to rs.getString(2) }.single()
        newly.forEach { (petBadgeId, code) ->
            jdbc.sql(
                """
                INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, published_at)
                VALUES (:id, 'ACHIEVEMENT', :ref, :owner, :pet, now()) ON CONFLICT (type, ref_id) DO NOTHING
                """,
            ).param("id", UUID.randomUUID()).param("ref", petBadgeId).param("owner", ownerId).param("pet", petId).update()
            val badge = jdbc.sql("SELECT name, emoji FROM badges WHERE code = :code").param("code", code)
                .query { rs, _ -> rs.getString("emoji") + " " + rs.getString("name") }.single()
            notifier.notify(ownerId, NotificationType.ACHIEVEMENT, "New badge for $petName", "$petName earned the $badge badge!", mapOf("petId" to petId.toString(), "badgeCode" to code))
            log.info("badge awarded pet={} badge={}", petId, code)
        }
    }

    private data class BadgeStats(val activities: Long, val distanceMeters: Long, val streakDays: Int, val posts: Long, val checkIns: Long, val playdates: Long)

    private fun badgeStats(petId: UUID): BadgeStats {
        val act = jdbc.sql("SELECT COUNT(*), COALESCE(SUM(distance_meters), 0) FROM activities WHERE pet_id = :pet")
            .param("pet", petId).query { rs, _ -> rs.getLong(1) to rs.getLong(2) }.single()
        val posts = jdbc.sql("SELECT COUNT(*) FROM posts WHERE pet_id = :pet").param("pet", petId).query(Long::class.java).single()
        val checkIns = jdbc.sql("SELECT COUNT(*) FROM check_ins WHERE pet_id = :pet").param("pet", petId).query(Long::class.java).single()
        val playdates = jdbc.sql(
            """
            SELECT (SELECT COUNT(*) FROM playdates WHERE organizer_pet_id = :pet AND status <> 'CANCELLED')
                 + (SELECT COUNT(*) FROM playdate_invites WHERE invitee_pet_id = :pet AND status = 'ACCEPTED')
            """,
        ).param("pet", petId).query(Long::class.java).single()
        return BadgeStats(act.first, act.second, streakDays(petId), posts, checkIns, playdates)
    }

    // ---- read models ----

    fun engagement(petId: UUID): PetEngagementDto {
        val xp = jdbc.sql("SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE pet_id = :pet").param("pet", petId)
            .query(Long::class.java).single()
        val level = levelFor(xp)
        val nextThreshold = xpForLevel(level + 1)
        val badges = jdbc.sql("SELECT COUNT(*) FROM pet_badges WHERE pet_id = :pet").param("pet", petId).query(Long::class.java).single()
        return PetEngagementDto(petId, xp, level, levelTitle(level), xp - xpForLevel(level), nextThreshold - xp, streakDays(petId), badges)
    }

    /** Consecutive days (UTC) with at least one logged activity, ending today or yesterday. */
    fun streakDays(petId: UUID): Int {
        val days = jdbc.sql(
            "SELECT DISTINCT started_at AT TIME ZONE 'UTC' AS d FROM activities WHERE pet_id = :pet AND started_at > now() - interval '400 days' ORDER BY d DESC",
        ).param("pet", petId).query { rs, _ -> rs.getDate(1).toLocalDate() }.list()
        if (days.isEmpty()) return 0
        val today = LocalDate.now(ZoneOffset.UTC)
        if (days.first() < today.minusDays(1)) return 0
        var streak = 1
        var cursor = days.first()
        for (day in days.drop(1)) {
            if (day == cursor.minusDays(1)) { streak++; cursor = day } else break
        }
        return streak
    }

    fun badges(petId: UUID): List<BadgeDto> = jdbc.sql(
        """
        SELECT b.code, b.name, b.description, b.emoji, pb.awarded_at
        FROM badges b LEFT JOIN pet_badges pb ON pb.badge_code = b.code AND pb.pet_id = :pet
        ORDER BY pb.awarded_at IS NULL, pb.awarded_at, b.code
        """,
    ).param("pet", petId).query { rs, _ ->
        BadgeDto(rs.getString("code"), rs.getString("name"), rs.getString("description"), rs.getString("emoji"),
            rs.getTimestamp("awarded_at") != null, rs.getTimestamp("awarded_at")?.toInstant())
    }.list()

    // ---- challenges ----

    @Transactional(readOnly = true)
    fun challenges(viewerId: UUID): List<ChallengeDto> {
        val rows = jdbc.sql(
            """
            SELECT c.*, cp.pet_id, cp.completed_at, p.name AS pet_name
            FROM challenges c
            LEFT JOIN challenge_participants cp ON cp.challenge_id = c.id AND cp.owner_id = :viewer
            LEFT JOIN pets p ON p.id = cp.pet_id
            WHERE c.ends_on >= CURRENT_DATE AND c.starts_on <= CURRENT_DATE + 7
            ORDER BY c.ends_on, c.id
            """,
        ).param("viewer", viewerId).query { rs, _ ->
            JoinedRow(challenge(rs), rs.getObject("pet_id", UUID::class.java), rs.getString("pet_name"), rs.getTimestamp("completed_at") != null)
        }.list()
        return rows.groupBy({ it.challenge.id }, { it }).map { (_, group) ->
            val challenge = group.first().challenge
            val joined = group.mapNotNull { row ->
                row.petId?.let {
                    ChallengeParticipantDto(it, row.petName ?: "",
                        challengeProgress(challenge.metric, it, challenge.startsOn, challenge.endsOn),
                        row.completed)
                }
            }
            challenge.copy(joined = joined)
        }
    }

    @Transactional
    fun joinChallenge(ownerId: UUID, challengeId: UUID, petId: UUID): ChallengeDto {
        petAccess.requireOwnedActivePet(ownerId, petId)
        val challenge = jdbc.sql("SELECT * FROM challenges WHERE id = :id").param("id", challengeId)
            .query { rs, _ -> challenge(rs) }.optional().orElseThrow { NotFoundException("Challenge not found") }
        val today = LocalDate.now(ZoneOffset.UTC)
        if (today.isAfter(challenge.endsOn)) throw ConflictException("This challenge already ended")
        val inserted = jdbc.sql(
            "INSERT INTO challenge_participants (challenge_id, pet_id, owner_id, joined_at) VALUES (:c, :pet, :owner, now()) ON CONFLICT DO NOTHING",
        ).param("c", challengeId).param("pet", petId).param("owner", ownerId).update()
        if (inserted == 0) throw ConflictException("This pet already joined the challenge")
        evaluateChallenges(petId)
        return challenges(ownerId).first { it.id == challengeId }
    }

    @Transactional
    fun createChallenge(adminId: UUID, request: CreateChallengeRequest): ChallengeDto {
        if (request.endsOn!!.isBefore(request.startsOn)) throw BadRequestException("endsOn must be on or after startsOn")
        val id = UUID.randomUUID()
        jdbc.sql(
            """
            INSERT INTO challenges (id, title, description, metric, target_value, xp_reward, starts_on, ends_on, author, created_by, created_at)
            VALUES (:id, :title, :description, :metric, :target, :xp, :starts, :ends, 'ADMIN', :admin, now())
            """,
        ).param("id", id).param("title", request.title!!.trim()).param("description", request.description!!.trim())
            .param("metric", request.metric!!.name).param("target", request.targetValue).param("xp", request.xpReward)
            .param("starts", request.startsOn).param("ends", request.endsOn).param("admin", adminId).update()
        log.info("challenge created challenge={} by={}", id, adminId)
        return jdbc.sql("SELECT * FROM challenges WHERE id = :id").param("id", id).query { rs, _ -> challenge(rs) }.single()
    }

    /** Completes joined challenges whose target is met; awards XP once via an idempotent xp event. */
    fun evaluateChallenges(petId: UUID) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val open = jdbc.sql(
            """
            SELECT c.id, c.metric, c.target_value, c.xp_reward, c.title, c.starts_on, c.ends_on, cp.owner_id
            FROM challenge_participants cp JOIN challenges c ON c.id = cp.challenge_id
            WHERE cp.pet_id = :pet AND cp.completed_at IS NULL AND c.starts_on <= :today AND c.ends_on >= :today
            """,
        ).param("pet", petId).param("today", today).query { rs, _ ->
            OngoingChallenge(rs.getObject("id", UUID::class.java), ChallengeMetric.valueOf(rs.getString("metric")),
                rs.getLong("target_value"), rs.getInt("xp_reward"), rs.getString("title"),
                rs.getDate("starts_on").toLocalDate(), rs.getDate("ends_on").toLocalDate(), rs.getObject("owner_id", UUID::class.java))
        }.list()
        open.forEach { ch ->
            if (challengeProgress(ch.metric, petId, ch.startsOn, ch.endsOn) >= ch.targetValue) {
                jdbc.sql("UPDATE challenge_participants SET completed_at = now() WHERE challenge_id = :c AND pet_id = :pet AND completed_at IS NULL")
                    .param("c", ch.id).param("pet", petId).update()
                awardXp(petId, ch.ownerId, "CHALLENGE", "challenge:${ch.id}:$petId", ch.xpReward, Instant.now())
                notifier.notify(ch.ownerId, NotificationType.CHALLENGE, "Challenge complete", "Your pet finished “${ch.title}” and earned ${ch.xpReward} XP!",
                    mapOf("challengeId" to ch.id.toString(), "petId" to petId.toString()))
            }
        }
    }

    private data class OngoingChallenge(val id: UUID, val metric: ChallengeMetric, val targetValue: Long, val xpReward: Int, val title: String, val startsOn: LocalDate, val endsOn: LocalDate, val ownerId: UUID)

    private data class JoinedRow(val challenge: ChallengeDto, val petId: UUID?, val petName: String?, val completed: Boolean)

    private fun challengeProgress(metric: ChallengeMetric, petId: UUID, startsOn: LocalDate, endsOn: LocalDate): Long {
        val column = when (metric) {
            ChallengeMetric.ACTIVITY_COUNT -> "COUNT(*)"
            ChallengeMetric.DISTANCE_METERS -> "COALESCE(SUM(distance_meters), 0)"
            ChallengeMetric.DURATION_SECONDS -> "COALESCE(SUM(duration_seconds), 0)"
        }
        return jdbc.sql("SELECT $column FROM activities WHERE pet_id = :pet AND started_at::date BETWEEN :starts AND :ends")
            .param("pet", petId).param("starts", startsOn).param("ends", endsOn).query(Long::class.java).single()
    }

    // ---- leaderboard ----

    @Transactional(readOnly = true)
    fun leaderboard(viewerId: UUID, scope: String): LeaderboardDto {
        val neighborhood = jdbc.sql("SELECT neighborhood FROM owners WHERE id = :id").param("id", viewerId)
            .query(String::class.java).optional().orElse(null)
        val effectiveScope = if (scope == "neighborhood" && neighborhood == null) "global" else scope
        val rows = jdbc.sql(
            """
            SELECT p.id, p.name, p.avatar_url, o.display_name, o.neighborhood,
                   SUM(x.points) AS weekly_xp, (p.owner_id = :viewer) AS own
            FROM xp_events x
            JOIN pets p ON p.id = x.pet_id AND p.status = 'ACTIVE'
            JOIN owners o ON o.id = p.owner_id AND o.status = 'ACTIVE'
            WHERE x.occurred_at > now() - interval '7 days'
              AND (:global OR lower(o.neighborhood) = lower(CAST(:nh AS text)))
            GROUP BY p.id, p.name, p.avatar_url, o.display_name, o.neighborhood, p.owner_id
            ORDER BY weekly_xp DESC, p.id LIMIT 50
            """,
        ).param("viewer", viewerId).param("global", effectiveScope == "global").param("nh", neighborhood)
            .query { rs, _ ->
                val petId = rs.getObject("id", UUID::class.java)
                LeaderboardEntryDto(0, petId, rs.getString("name"), rs.getString("avatar_url"), rs.getString("display_name"),
                    rs.getString("neighborhood"), 0, "", rs.getLong("weekly_xp"), rs.getBoolean("own"))
            }.list()
        val totals = jdbc.sql("SELECT pet_id, SUM(points) FROM xp_events WHERE pet_id IN (:ids) GROUP BY pet_id")
            .param("ids", rows.map { it.petId }.ifEmpty { listOf(UUID(0, 0)) }.toSet())
            .query { rs, _ -> rs.getObject(1, UUID::class.java) to rs.getLong(2) }.list().toMap()
        val entries = rows.mapIndexed { index, e ->
            val level = levelFor(totals[e.petId] ?: 0)
            e.copy(rank = index + 1, level = level, levelTitle = levelTitle(level))
        }
        return LeaderboardDto(effectiveScope, entries)
    }

    private fun challenge(rs: ResultSet) = ChallengeDto(
        rs.getObject("id", UUID::class.java), rs.getString("title"), rs.getString("description"),
        ChallengeMetric.valueOf(rs.getString("metric")), rs.getLong("target_value"), rs.getInt("xp_reward"),
        rs.getDate("starts_on").toLocalDate(), rs.getDate("ends_on").toLocalDate(), rs.getString("author"), emptyList(),
    )
}
