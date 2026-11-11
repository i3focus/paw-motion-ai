package com.pawmotionai.activity.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.pawmotionai.activity.application.port.out.ActivityRepository;
import com.pawmotionai.activity.domain.model.Activity;

@Repository
public class ActivityPersistenceAdapter implements ActivityRepository {

    private final ActivityJpaRepository repository;

    public ActivityPersistenceAdapter(ActivityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Activity save(Activity activity) {
        ActivityJpaEntity entity = repository.findById(activity.getId())
            .orElseGet(() -> new ActivityJpaEntity(
                activity.getId(), activity.getOwnerId(), activity.getPetId(), activity.getType(), activity.getStartedAt(),
                activity.getDurationSeconds(), activity.getDistanceMeters(), activity.getNotes(), activity.getCreatedAt(), Instant.now(),
                activity.getLatitude(), activity.getLongitude(), activity.isShareToPawCity()));
        return toDomain(repository.save(entity));
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
