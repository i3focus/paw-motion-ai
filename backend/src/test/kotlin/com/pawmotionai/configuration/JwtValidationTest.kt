package com.pawmotionai.configuration

import com.pawmotionai.identity.adapter.out.security.JwtTokenIssuer
import com.pawmotionai.identity.domain.model.Owner
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Instant
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.security.oauth2.jwt.JwtException

class JwtValidationTest {
    private val key = SecretKeySpec("unit-test-signing-key-0123456789-abcdef".toByteArray(), "HmacSHA256")
    private val encoder = NimbusJwtEncoder.withSecretKey(key).build()
    private val issuer = "https://api.pawmotion.ai"
    private val audience = "paw-motion-ai-api"

    private fun decoder(deadline: Instant = Instant.now().plusSeconds(3600)): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key).build().also {
            it.setJwtValidator(JwtValidation(issuer, audience, setOf("paw-motion-ai"), deadline))
        }

    private fun mint(iss: String? = issuer, aud: String? = audience, exp: Instant? = Instant.now().plusSeconds(600)): String {
        val b = JwtClaimsSet.builder().subject(UUID.randomUUID().toString()).issuedAt((exp ?: Instant.now()).minusSeconds(60)).claim("roles", listOf("OWNER"))
        if (iss != null) b.issuer(iss)
        if (aud != null) b.audience(listOf(aud))
        if (exp != null) b.expiresAt(exp)
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), b.build())).tokenValue
    }

    @Test
    fun `token emitido pelo JwtTokenIssuer e aceito pelo decoder`() {
        val owner = Owner.register(UUID.randomUUID(), "a@b.com", "hash", "Ana", Instant.now())
        val token = JwtTokenIssuer(encoder, issuer, audience, 3600).issue(owner).accessToken
        val jwt = decoder().decode(token)
        assertEquals(issuer, jwt.claims["iss"].toString())
        assertEquals(listOf(audience), jwt.audience)
    }

    @Test
    fun `rejeita iss estrangeiro, aud estrangeira e exp ausente`() {
        assertFailsWith<JwtException> { decoder().decode(mint(iss = "https://evil.example")) }
        assertFailsWith<JwtException> { decoder().decode(mint(aud = "outro-servico")) }
        assertFailsWith<JwtException> { decoder().decode(mint(exp = null)) }
        assertFailsWith<JwtException> { decoder().decode(mint(exp = Instant.now().minusSeconds(3600))) }
    }

    @Test
    fun `janela de compatibilidade aceita token legado sem aud so ate o prazo`() {
        val legacy = mint(iss = "paw-motion-ai", aud = null)
        decoder(Instant.now().plusSeconds(60)).decode(legacy)
        assertFailsWith<JwtException> { decoder(Instant.now().minusSeconds(1)).decode(legacy) }
        assertFailsWith<JwtException> { decoder().decode(mint(iss = "https://evil.example", aud = null)) }
        assertFailsWith<JwtException> { decoder().decode(mint(iss = null, aud = null)) }
    }

    @Test
    fun `token legado com aud errada e rejeitado mesmo na janela`() {
        assertFailsWith<JwtException> { decoder().decode(mint(iss = "paw-motion-ai", aud = "outro")) }
    }
}
