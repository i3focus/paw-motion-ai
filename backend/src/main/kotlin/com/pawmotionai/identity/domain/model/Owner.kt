package com.pawmotionai.identity.domain.model

import java.time.Instant
import java.util.UUID

enum class OwnerRole {
    OWNER,
    PARTNER,
    ADMIN
}

enum class OwnerStatus {
    ACTIVE,
    DISABLED
}

class Owner private constructor(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    displayName: String,
    val role: OwnerRole,
    val status: OwnerStatus,
    val createdAt: Instant,
    bio: String?,
    phone: String?,
    avatarUrl: String?,
) {

    var displayName: String = displayName
        private set
    var bio: String? = bio
        private set
    var phone: String? = phone
        private set
    var avatarUrl: String? = avatarUrl
        private set

    val isActive: Boolean get() = status == OwnerStatus.ACTIVE
    val isPartner: Boolean get() = role == OwnerRole.PARTNER

    fun updateProfile(displayName: String?, bio: String?, phone: String?, avatarUrl: String?) {
        if (displayName != null) {
            val trimmedName = displayName.trim()
            require(trimmedName.isNotEmpty()) { "Display name cannot be blank" }
            this.displayName = trimmedName
        }
        if (bio != null) {
            this.bio = trimToNull(bio)
        }
        if (phone != null) {
            this.phone = trimToNull(phone)
        }
        if (avatarUrl != null) {
            this.avatarUrl = trimToNull(avatarUrl)
        }
    }

    companion object {
        fun register(id: UUID, email: String, passwordHash: String, displayName: String, createdAt: Instant): Owner =
            Owner(id, email, passwordHash, displayName, OwnerRole.OWNER, OwnerStatus.ACTIVE, createdAt, null, null, null)

        fun registerPartner(id: UUID, email: String, passwordHash: String, displayName: String, createdAt: Instant): Owner =
            Owner(id, email, passwordHash, displayName, OwnerRole.PARTNER, OwnerStatus.ACTIVE, createdAt, null, null, null)

        fun reconstitute(id: UUID, email: String, passwordHash: String, displayName: String, role: OwnerRole,
                         status: OwnerStatus, createdAt: Instant, bio: String?, phone: String?, avatarUrl: String?): Owner =
            Owner(id, email, passwordHash, displayName, role, status, createdAt, bio, phone, avatarUrl)

        private fun trimToNull(value: String): String? = value.trim().ifEmpty { null }
    }
}
