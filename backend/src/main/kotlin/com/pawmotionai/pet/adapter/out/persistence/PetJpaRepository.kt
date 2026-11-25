package com.pawmotionai.pet.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PetJpaRepository : JpaRepository<PetJpaEntity, UUID> {

    fun findByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<PetJpaEntity>

    @Query(value = "SELECT id FROM owners WHERE id = :id FOR UPDATE", nativeQuery = true)
    fun lockOwner(@Param("id") id: UUID): List<UUID>

    @Query(value = "SELECT COUNT(*) FROM pets WHERE owner_id = :id AND status = 'ACTIVE'", nativeQuery = true)
    fun countActiveByOwnerId(@Param("id") id: UUID): Long

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): PetJpaEntity?
}
