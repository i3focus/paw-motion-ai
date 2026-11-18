package com.pawmotionai.pet.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PetJpaRepository : JpaRepository<PetJpaEntity, UUID> {

    fun findByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<PetJpaEntity>

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): PetJpaEntity?
}
