package com.pawmotionai.configuration

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class StartupSecurityValidatorTest {
    private val strong = "Zq8!vLm2#Rt5@Xw9\$Ke4&Yb7^Hn3*Pd6"

    @Test
    fun `accepts a strong secret and provider configuration outside local`() {
        val env = MockEnvironment().withProperty("app.security.jwt.secret", strong)
            .withProperty("app.billing.provider", "abacatepay")
            .withProperty("app.billing.abacatepay.api-key", "k").withProperty("app.billing.abacatepay.webhook-secret", "w")
        StartupSecurityValidator(env)
    }

    @Test
    fun `rejects weak secrets by rule`() {
        assertTrue(StartupSecurityValidator.jwtSecretViolation(null)!!.contains("set"))
        assertTrue(StartupSecurityValidator.jwtSecretViolation("short")!!.contains("32 bytes"))
        assertTrue(StartupSecurityValidator.jwtSecretViolation("replace-with-" + "a1b2c3d4e5f6g7h8i9j0k1l2")!!.contains("placeholder"))
        assertTrue(StartupSecurityValidator.jwtSecretViolation("ab".repeat(20))!!.contains("distinct"))
        assertNull(StartupSecurityValidator.jwtSecretViolation(strong))
    }

    @Test
    fun `local profile may use the fake provider and error text never contains the secret`() {
        val weak = "ababababababababababababababababab"
        val env = MockEnvironment().withProperty("app.security.jwt.secret", weak).withProperty("app.billing.provider", "fake")
        env.setActiveProfiles("local")
        val error = assertThrows(IllegalStateException::class.java) { StartupSecurityValidator(env) }
        assertFalse(error.message!!.contains(weak))
        assertTrue(error.message!!.contains("app.security.jwt.secret"))
        env.setProperty("app.security.jwt.secret", strong)
        StartupSecurityValidator(env)
    }

    @Test
    fun `outside local a non real provider is rejected`() {
        val env = MockEnvironment().withProperty("app.security.jwt.secret", strong).withProperty("app.billing.provider", "fake")
        val error = assertThrows(IllegalStateException::class.java) { StartupSecurityValidator(env) }
        assertTrue(error.message!!.contains("app.billing.provider"))
    }
}
