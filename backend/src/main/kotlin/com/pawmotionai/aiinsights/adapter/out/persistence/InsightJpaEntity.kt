package com.pawmotionai.aiinsights.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "insights")
class InsightJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,

    @Column(name = "pet_id")
    var petId: UUID? = null,

    @Column(nullable = false, length = 1000)
    var text: String,

    @Column(nullable = false)
    var provider: String,

    var model: String? = null,

    @Column(name = "fallback_used", nullable = false)
    var fallbackUsed: Boolean,

    @Column(name = "generated_at", nullable = false)
    var generatedAt: Instant,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    var version: Long? = null,
)
