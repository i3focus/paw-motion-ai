package com.pawmotionai.identity.adapter.out.security

import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.application.port.out.TokenIssuer.IssuedToken
import com.pawmotionai.identity.domain.model.Owner
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JwtTokenIssuer(
    private val jwtEncoder: JwtEncoder,
    @param:Value("\${app.security.jwt.issuer}") private val issuer: String,
    @param:Value("\${app.security.jwt.audience}") private val audience: String,
    @param:Value("\${app.security.jwt.ttl-seconds}") private val ttlSeconds: Long,
) : TokenIssuer {

    override fun issue(owner: Owner): IssuedToken {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(listOf(audience))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(ttlSeconds))
            .subject(owner.id.toString())
            .claim("email", owner.email)
            .claim("displayName", owner.displayName)
            .claim("role", owner.role.name)
            .claim("roles", listOf(owner.role.name))
            .build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue
        return IssuedToken(token, "Bearer", ttlSeconds)
    }
}
