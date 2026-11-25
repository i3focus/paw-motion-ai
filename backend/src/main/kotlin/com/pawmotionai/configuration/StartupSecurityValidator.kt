package com.pawmotionai.configuration

import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Fail-closed startup checks (audit findings C1 and C2). It is a dependency of `jwtDecoder` and `jwtEncoder`, so it runs
 * before either of them is created. Messages name the rule and the configuration key, never the configured value.
 */
@Component
class StartupSecurityValidator(environment: Environment) {
    init {
        val violations = buildList {
            jwtSecretViolation(environment.getProperty(JWT_SECRET_KEY))?.let { add("$JWT_SECRET_KEY (JWT_SECRET): $it") }
            if (!environment.acceptsProfiles(Profiles.of(LOCAL_PROFILE))) addAll(billingViolations(environment))
        }
        check(violations.isEmpty()) {
            "Insecure startup configuration rejected: " + violations.joinToString("; ")
        }
    }

    private fun billingViolations(environment: Environment): List<String> {
        val provider = environment.getProperty(PROVIDER_KEY).orEmpty().trim()
        if (!provider.equals("abacatepay", ignoreCase = true)) {
            return listOf("$PROVIDER_KEY (PAYMENT_PROVIDER): outside the 'local' profile the payment provider must be 'abacatepay'")
        }
        return listOfNotNull(
            "app.billing.abacatepay.api-key (ABACATEPAY_API_KEY): required when the provider is 'abacatepay'"
                .takeIf { environment.getProperty("app.billing.abacatepay.api-key").isNullOrBlank() },
            "app.billing.abacatepay.webhook-secret (ABACATEPAY_WEBHOOK_SECRET): required when the provider is 'abacatepay'"
                .takeIf { environment.getProperty("app.billing.abacatepay.webhook-secret").isNullOrBlank() },
        )
    }

    companion object {
        const val JWT_SECRET_KEY = "app.security.jwt.secret"
        private const val PROVIDER_KEY = "app.billing.provider"
        private const val LOCAL_PROFILE = "local"
        private const val MIN_SECRET_BYTES = 32
        private const val MIN_DISTINCT_CHARS = 12
        private const val PLACEHOLDER_PREFIX = "replace-with-"

        /** Returns the broken rule, or null when the secret is acceptable. */
        fun jwtSecretViolation(secret: String?): String? = when {
            secret.isNullOrBlank() -> "must be set"
            secret.toByteArray(StandardCharsets.UTF_8).size < MIN_SECRET_BYTES -> "must have at least $MIN_SECRET_BYTES bytes (UTF-8)"
            secret.startsWith(PLACEHOLDER_PREFIX, ignoreCase = true) -> "must not be a documented placeholder value"
            secret.toSet().size < MIN_DISTINCT_CHARS -> "must contain at least $MIN_DISTINCT_CHARS distinct characters"
            else -> null
        }
    }
}
