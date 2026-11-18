package com.pawmotionai.activity.adapter.out.persistence

import com.pawmotionai.activity.domain.model.ActivityType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "activities")
class ActivityJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,

    @Column(name = "pet_id", nullable = false)
    var petId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ActivityType,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant,

    @Column(name = "duration_seconds", nullable = false)
    var durationSeconds: Long,

    @Column(name = "distance_meters")
    var distanceMeters: Double? = null,

    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column
    var latitude: Double? = null,

    @Column
    var longitude: Double? = null,

    @Column(name = "share_to_paw_city", nullable = false)
    var shareToPawCity: Boolean = false,

    @Version
    var version: Long? = null,
)
