package com.pawmotionai.shared

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

val Jwt.ownerId: UUID get() = UUID.fromString(subject)

val Jwt.role: String get() = getClaimAsString("role") ?: "OWNER"

fun Jwt.requireRole(vararg roles: String) {
    if (role !in roles) throw ForbiddenException("This action requires one of: ${roles.joinToString()}")
}
