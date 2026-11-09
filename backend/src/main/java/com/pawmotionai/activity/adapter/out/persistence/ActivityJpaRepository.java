package com.pawmotionai.activity.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityJpaRepository extends JpaRepository<ActivityJpaEntity, UUID> {

    List<ActivityJpaEntity> findByOwnerIdOrderByStartedAtDesc(UUID ownerId);

    Optional<ActivityJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
