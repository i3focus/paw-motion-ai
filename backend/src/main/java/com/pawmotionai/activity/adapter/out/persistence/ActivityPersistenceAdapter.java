package com.pawmotionai.activity.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pawmotionai.activity.application.port.out.ActivityRepository;
import com.pawmotionai.activity.domain.model.Activity;
import com.pawmotionai.engagement.EngagementService;

@Repository
public class ActivityPersistenceAdapter implements ActivityRepository {

    private final ActivityJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final EngagementService engagement;

    public ActivityPersistenceAdapter(ActivityJpaRepository repository, JdbcTemplate jdbcTemplate, EngagementService engagement) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.engagement = engagement;
    }

    @Override
    public Activity save(Activity activity) {
        ActivityJpaEntity entity = repository.findById(activity.getId())
            .orElseGet(() -> new ActivityJpaEntity(
                activity.getId(), activity.getOwnerId(), activity.getPetId(), activity.getType(), activity.getStartedAt(),
                activity.getDurationSeconds(), activity.getDistanceMeters(), activity.getNotes(), activity.getCreatedAt(), Instant.now(),
                activity.getLatitude(), activity.getLongitude(), activity.isShareToPawCity()));
        Activity saved = toDomain(repository.saveAndFlush(entity));
        if (saved.isShareToPawCity()) {
            publishToFeed(saved);
        }
        engagement.recordActivity(saved.getId(), saved.getOwnerId(), saved.getPetId(), saved.getDistanceMeters(), saved.getStartedAt());
        return saved;
    }

    private void publishToFeed(Activity activity) {
        jdbcTemplate.update("""
            INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, location, published_at)
            VALUES (?::uuid, 'ACTIVITY', ?::uuid, ?::uuid, ?::uuid,
                    CASE WHEN ?::float8 IS NULL OR ?::float8 IS NULL THEN NULL
                         ELSE ST_SetSRID(ST_MakePoint(?::float8, ?::float8), 4326)::geography END, ?)
            ON CONFLICT (type, ref_id) DO NOTHING
            """, UUID.randomUUID().toString(), activity.getId().toString(), activity.getOwnerId().toString(),
            activity.getPetId().toString(), activity.getLongitude(), activity.getLatitude(), activity.getLongitude(),
            activity.getLatitude(), java.sql.Timestamp.from(activity.getStartedAt()));
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Activity> findByOwnerIdOrderByStartedAtDesc(UUID ownerId) {
        return repository.findByOwnerIdOrderByStartedAtDesc(ownerId).stream().map(this::toDomain).toList();
    }

    private Activity toDomain(ActivityJpaEntity entity) {
        return Activity.reconstitute(
            entity.getId(), entity.getOwnerId(), entity.getPetId(), entity.getType(), entity.getStartedAt(),
            entity.getDurationSeconds(), entity.getDistanceMeters(), entity.getNotes(), entity.getCreatedAt(),
            entity.getLatitude(), entity.getLongitude(), entity.isShareToPawCity());
    }
}
