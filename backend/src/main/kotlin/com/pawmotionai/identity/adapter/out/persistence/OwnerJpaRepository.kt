package com.pawmotionai.identity.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OwnerJpaRepository : JpaRepository<OwnerJpaEntity, UUID> {

    fun findByEmailIgnoreCase(email: String): OwnerJpaEntity?

    fun existsByEmailIgnoreCase(email: String): Boolean
}
