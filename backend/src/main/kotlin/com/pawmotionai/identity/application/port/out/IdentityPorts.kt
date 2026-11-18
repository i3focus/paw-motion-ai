package com.pawmotionai.identity.application.port.out

import com.pawmotionai.identity.domain.model.Owner
import java.util.UUID

interface OwnerRepository {

    fun save(owner: Owner): Owner

    fun findById(id: UUID): Owner?

    fun findByEmailIgnoreCase(email: String): Owner?

    fun existsByEmailIgnoreCase(email: String): Boolean
}

interface PasswordHasher {

    fun hash(rawPassword: String): String

    fun matches(rawPassword: String, encodedPassword: String): Boolean
}

interface TokenIssuer {

    fun issue(owner: Owner): IssuedToken

    data class IssuedToken(val accessToken: String, val tokenType: String, val expiresInSeconds: Long)
}
