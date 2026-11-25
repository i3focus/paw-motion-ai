package com.pawmotionai.identity.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.pawmotionai.configuration.StartupSecurityValidator
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Configuration
class IdentityConfiguration {

    @Bean
    fun jwtEncoder(@Value("\${app.security.jwt.secret}") secret: String, startupSecurityValidator: StartupSecurityValidator): JwtEncoder {
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        return NimbusJwtEncoder.withSecretKey(key).build()
    }
}
