package com.pawmotionai.activity.adapter.out.persistence

import com.pawmotionai.activity.application.port.out.ActivityRepository
import com.pawmotionai.activity.domain.model.Activity
import com.pawmotionai.engagement.EngagementService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import com.pawmotionai.activity.application.port.out.WeeklyTotals
import com.pawmotionai.activity.domain.model.ActivityType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Repository
class ActivityPersistenceAdapter(
    private val repository: ActivityJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val engagement: EngagementService,
) : ActivityRepository {

    override fun save(activity: Activity): Activity {
        val entity = repository.findById(activity.id)
            .orElseGet {
                ActivityJpaEntity(
                    activity.id, activity.ownerId, activity.petId, activity.type, activity.startedAt,
                    activity.durationSeconds, activity.distanceMeters, activity.notes, activity.createdAt, Instant.now(),
                    activity.latitude, activity.longitude, activity.shareToPawCity)
            }
        val saved = toDomain(repository.saveAndFlush(entity))
        if (saved.shareToPawCity) {
            publishToFeed(saved)
        }
        engagement.recordActivity(saved.id, saved.ownerId, saved.petId, saved.distanceMeters, saved.startedAt)
        return saved
    }

    private fun publishToFeed(activity: Activity) {
        jdbcTemplate.update("""
            INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, location, published_at)
            VALUES (?::uuid, 'ACTIVITY', ?::uuid, ?::uuid, ?::uuid,
                    CASE WHEN ?::float8 IS NULL OR ?::float8 IS NULL THEN NULL
                         ELSE ST_SetSRID(ST_MakePoint(?::float8, ?::float8), 4326)::geography END, ?)
            ON CONFLICT (type, ref_id) DO NOTHING
            """, UUID.randomUUID().toString(), activity.id.toString(), activity.ownerId.toString(),
            activity.petId.toString(), activity.longitude, activity.latitude, activity.longitude,
            activity.latitude, Timestamp.from(activity.startedAt))
    }

    override fun findById(id: UUID): Activity? = repository.findById(id).map(::toDomain).orElse(null)

    override fun findByOwnerIdOrderByStartedAtDesc(ownerId: UUID): List<Activity> =
        repository.findByOwnerIdOrderByStartedAtDesc(ownerId).map(::toDomain)

    override fun findFiltered(ownerId: UUID, petId: UUID?, from: Instant?, to: Instant?, limit: Int): List<Activity> {
        val sql = StringBuilder("""
            SELECT id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, notes, created_at,
                   latitude, longitude, share_to_paw_city
            FROM activities WHERE owner_id = ?""")
        val args = mutableListOf<Any>(ownerId)
        if (petId != null) { sql.append(" AND pet_id = ?"); args.add(petId) }
        if (from != null) { sql.append(" AND started_at >= ?"); args.add(Timestamp.from(from)) }
        if (to != null) { sql.append(" AND started_at <= ?"); args.add(Timestamp.from(to)) }
        sql.append(" ORDER BY started_at DESC LIMIT ?")
        args.add(limit)
        return jdbcTemplate.query(sql.toString(), { rs, _ ->
            Activity.reconstitute(
                rs.getObject("id", UUID::class.java), rs.getObject("owner_id", UUID::class.java),
                rs.getObject("pet_id", UUID::class.java), ActivityType.valueOf(rs.getString("type")),
                rs.getTimestamp("started_at").toInstant(), rs.getLong("duration_seconds"),
                rs.getObject("distance_meters") as Double?, rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("latitude") as Double?, rs.getObject("longitude") as Double?,
                rs.getBoolean("share_to_paw_city"))
        }, *args.toTypedArray())
    }

    override fun weeklyTotals(ownerId: UUID, since: Instant): WeeklyTotals =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*), COALESCE(SUM(distance_meters), 0), COALESCE(SUM(duration_seconds), 0) " +
                "FROM activities WHERE owner_id = ? AND started_at >= ?",
            { rs, _ -> WeeklyTotals(rs.getInt(1), rs.getDouble(2), rs.getLong(3)) },
            ownerId, Timestamp.from(since))!!

    override fun activeDays(ownerId: UUID): Set<LocalDate> =
        jdbcTemplate.query(
            "SELECT DISTINCT (started_at AT TIME ZONE 'UTC')::date FROM activities WHERE owner_id = ?",
            { rs, _ -> rs.getObject(1, LocalDate::class.java) }, ownerId).toSet()

    private fun toDomain(entity: ActivityJpaEntity): Activity =
        Activity.reconstitute(
            entity.id, entity.ownerId, entity.petId, entity.type, entity.startedAt,
            entity.durationSeconds, entity.distanceMeters, entity.notes, entity.createdAt,
            entity.latitude, entity.longitude, entity.shareToPawCity)
}
