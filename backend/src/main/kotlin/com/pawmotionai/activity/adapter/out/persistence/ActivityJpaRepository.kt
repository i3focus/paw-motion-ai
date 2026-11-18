package com.pawmotionai.activity.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ActivityJpaRepository : JpaRepository<ActivityJpaEntity, UUID> {

    fun findByOwnerIdOrderByStartedAtDesc(ownerId: UUID): List<ActivityJpaEntity>

    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): ActivityJpaEntity?
}
