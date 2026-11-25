package com.pawmotionai.configuration

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import java.time.Clock
import java.time.Instant

/**
 * N14: validacao de `exp` (obrigatorio), `iss` e `aud` dos JWT proprios.
 *
 * Janela de compatibilidade: ate [legacyDeadline], tokens emitidos antes desta versao (sem `aud`, e com `iss` igual a um dos
 * [legacyIssuers]) continuam aceitos para nao derrubar sessoes. Depois dela, `aud` e obrigatoria. Tokens que trazem `aud`
 * errada ou `iss` desconhecido sao sempre rejeitados.
 */
class JwtValidation(
    private val issuer: String,
    private val audience: String,
    private val legacyIssuers: Set<String>,
    private val legacyDeadline: Instant,
    private val clock: Clock = Clock.systemUTC(),
) : OAuth2TokenValidator<Jwt> {
    private val delegate: OAuth2TokenValidator<Jwt> = JwtValidators.createDefault()

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val base = delegate.validate(token)
        if (base.hasErrors()) return base
        if (token.expiresAt == null) return failure("exp is required")
        val tokenIssuer = token.claims["iss"]?.toString()
        val audiences = token.audience
        val legacyWindow = clock.instant().isBefore(legacyDeadline)
        if (audiences.isNullOrEmpty()) {
            val legacyOk = legacyWindow && tokenIssuer != null && (tokenIssuer == issuer || tokenIssuer in legacyIssuers)
            return if (legacyOk) OAuth2TokenValidatorResult.success() else failure("aud is required")
        }
        if (tokenIssuer != issuer) return failure("iss mismatch")
        if (audience !in audiences) return failure("aud mismatch")
        return OAuth2TokenValidatorResult.success()
    }

    private fun failure(description: String) =
        OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", description, null))
}
