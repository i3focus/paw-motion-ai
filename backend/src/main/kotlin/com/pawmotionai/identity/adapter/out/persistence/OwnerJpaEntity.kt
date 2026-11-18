package com.pawmotionai.identity.adapter.out.persistence

import com.pawmotionai.identity.domain.model.OwnerRole
import com.pawmotionai.identity.domain.model.OwnerStatus
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
@Table(name = "owners")
class OwnerJpaEntity(

    @Id
    var id: UUID,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: OwnerRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OwnerStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column
    var bio: String? = null,

    @Column
    var phone: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Version
    var version: Long? = null,
) {

    fun updateProfile(displayName: String, bio: String?, phone: String?, avatarUrl: String?) {
        this.displayName = displayName
        this.bio = bio
        this.phone = phone
        this.avatarUrl = avatarUrl
        this.updatedAt = Instant.now()
    }
}
