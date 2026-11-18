package com.pawmotionai.identity.adapter.`in`.rest

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class RegisterOwnerRequest(
    @field:NotBlank @field:Email @field:Size(max = 320) val email: String,
    @field:NotBlank @field:Size(min = 8, max = 128)
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
        message = "must include uppercase and lowercase letters, a number and a symbol")
    val password: String,
    @field:NotBlank @field:Size(max = 120) val displayName: String,
)

data class RegisterPartnerRequest(
    @field:NotBlank @field:Email @field:Size(max = 320) val email: String,
    @field:NotBlank @field:Size(min = 8, max = 128)
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
        message = "must include uppercase and lowercase letters, a number and a symbol")
    val password: String,
    @field:NotBlank @field:Size(max = 120) val displayName: String,
    @field:NotBlank @field:Size(max = 160) val businessName: String,
    @field:Size(max = 300) val website: String?,
    @field:Size(max = 1000) val description: String?,
)

data class UpdateProfileRequest(
    @field:Size(max = 120) val displayName: String?,
    @field:Size(max = 500) val bio: String?,
    @field:Size(max = 40) val phone: String?,
    @field:Size(max = 500) val avatarUrl: String?,
)
