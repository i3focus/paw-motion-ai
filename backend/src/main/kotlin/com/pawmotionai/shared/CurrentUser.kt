package com.pawmotionai.shared

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

val Jwt.ownerId: UUID get() = UUID.fromString(subject)
