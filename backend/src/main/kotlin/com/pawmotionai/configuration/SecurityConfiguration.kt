package com.pawmotionai.configuration

import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.shared.SecurityRoles
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity, corsConfigurationSource: CorsConfigurationSource,
                            ownerRepository: OwnerRepository,
                            @Value("\${app.security.trusted-proxies}") trustedProxies: String,
                            @Value("\${app.rate-limit.mode:shadow}") rateLimitMode: String,
                            @Value("\${app.rate-limit.webhook-per-minute:600}") webhookPerMinute: Int,
                            @Value("\${app.rate-limit.read.mode:enforce}") readMode: String,
                            @Value("\${app.rate-limit.read.per-minute:600}") readPerMinute: Int,
                            @Value("\${app.rate-limit.read.costly-per-minute:240}") readCostlyPerMinute: Int,
                            meterRegistry: ObjectProvider<MeterRegistry>): SecurityFilterChain {
        val clientIpResolver = ClientIpResolver(trustedProxies)
        val registry = meterRegistry.getIfAvailable()
        val settings = RateLimitSettings(RateLimitSettings.Mode.parse(rateLimitMode), webhookPerMinute)
        val readSettings = ReadLimitSettings(RateLimitSettings.Mode.parse(readMode), readPerMinute, readCostlyPerMinute)
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize -> authorize
                .requestMatchers("/api/auth/**", "/api/partner/auth/**", "/api/webhooks/payments/**", "/actuator/health", "/actuator/health/**", "/error",
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // N4: health e publico (sem detalhes); info, metrics e o resto de /actuator exigem ADMIN
                .requestMatchers("/actuator/**").hasAuthority(SecurityRoles.ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/media/*", "/api/vet-share/*").permitAll()
                .anyRequest().authenticated()
            }
            .addFilterBefore(AuthRateLimitFilter(clientIpResolver, registry), BearerTokenAuthenticationFilter::class.java)
            .addFilterAfter(WriteRateLimitFilter(clientIpResolver, settings, registry), BearerTokenAuthenticationFilter::class.java)
            .addFilterAfter(ReadRateLimitFilter(clientIpResolver, readSettings, registry), BearerTokenAuthenticationFilter::class.java)
            .addFilterAfter(ActiveAccountFilter(ownerRepository), BearerTokenAuthenticationFilter::class.java)
            .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) } }
            .build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authoritiesConverter = JwtGrantedAuthoritiesConverter()
        authoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM)
        authoritiesConverter.setAuthorityPrefix(ROLE_PREFIX)
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        return converter
    }

    @Bean
    fun jwtDecoder(@Value("\${app.security.jwt.secret}") secret: String, startupSecurityValidator: StartupSecurityValidator): JwtDecoder {
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key).build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(@Value("\${app.cors.allowed-origins}") allowedOrigins: String): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Idempotency-Key")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private companion object {
        const val ROLES_CLAIM = "roles"
        const val ROLE_PREFIX = "ROLE_"
    }
}
