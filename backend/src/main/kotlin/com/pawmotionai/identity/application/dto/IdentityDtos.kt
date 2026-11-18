package com.pawmotionai.identity.application.dto

import com.pawmotionai.identity.domain.model.Owner
import java.util.UUID

data class AuthResponseDto(val accessToken: String, val tokenType: String, val expiresInSeconds: Long,
                           val owner: OwnerResponseDto)

data class LoginCommand(val email: String, val password: String)

data class RegisterOwnerCommand(val email: String, val password: String, val displayName: String)

data class RegisterPartnerCommand(val email: String, val password: String, val displayName: String,
                                  val businessName: String?, val website: String?, val description: String?)

data class UpdateProfileCommand(val displayName: String?, val bio: String?, val phone: String?, val avatarUrl: String?)

data class OwnerResponseDto(val id: UUID, val email: String, val displayName: String, val role: String, val bio: String?,
                            val phone: String?, val avatarUrl: String?) {

    companion object {
        fun from(owner: Owner): OwnerResponseDto =
            OwnerResponseDto(owner.id, owner.email, owner.displayName, owner.role.name, owner.bio, owner.phone,
                owner.avatarUrl)
    }
}
