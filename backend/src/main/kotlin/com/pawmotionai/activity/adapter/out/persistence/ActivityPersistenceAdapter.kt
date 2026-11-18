package com.pawmotionai.activity.adapter.out.persistence

import com.pawmotionai.activity.application.port.out.ActivityRepository
import com.pawmotionai.activity.domain.model.Activity
import com.pawmotionai.engagement.EngagementService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
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

    private fun toDomain(entity: ActivityJpaEntity): Activity =
        Activity.reconstitute(
            entity.id, entity.ownerId, entity.petId, entity.type, entity.startedAt,
            entity.durationSeconds, entity.distanceMeters, entity.notes, entity.createdAt,
            entity.latitude, entity.longitude, entity.shareToPawCity)
}
