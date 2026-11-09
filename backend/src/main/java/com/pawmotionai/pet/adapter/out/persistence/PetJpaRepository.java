package com.pawmotionai.pet.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PetJpaRepository extends JpaRepository<PetJpaEntity, UUID> {

    List<PetJpaEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<PetJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
