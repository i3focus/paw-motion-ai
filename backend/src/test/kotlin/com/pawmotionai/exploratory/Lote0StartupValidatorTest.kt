package com.pawmotionai.exploratory

import com.pawmotionai.PawMotionAiApplication
import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Lote 0, PR 0a (C1, C2): startup validator. Corrected, so these tests run in the default suite.
 * Spec: security-audit-remediation, task 4.1.
 */
class Lote0StartupValidatorTest {
    private val repoRoot: Path = Path.of("..").toAbsolutePath().normalize()
    private fun envExampleValue(key: String): String =
        Files.readAllLines(repoRoot.resolve(".env.example")).firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")?.trim() ?: ""

    private fun rootMessages(failure: Throwable?): String =
        generateSequence(failure) { it.cause }.joinToString(" | ") { it.message.orEmpty() }.lowercase()

    private fun boot(vararg extra: String): ConfigurableApplicationContext {
        val postgres = ApiIntegrationTest.postgres
        // Command-line arguments have the highest precedence, so they override application.yml.
        val args = mutableListOf(
            "--spring.main.web-application-type=none",
            "--spring.datasource.url=${postgres.jdbcUrl}",
            "--spring.datasource.username=${postgres.username}",
            "--spring.datasource.password=${postgres.password}",
            "--spring.profiles.active=prod",
            "--app.billing.reconciliation-interval-ms=3600000",
            "--app.campaign.expiration-interval-ms=3600000",
        )
        args += extra.map { "--$it" }
        return SpringApplicationBuilder(PawMotionAiApplication::class.java).run(*args.toTypedArray())
    }

    /** Control: a strong secret with a real provider boots, so a failure in the tests below is caused by the rejected setting. */
    @Test
    fun `control boot with a strong secret and a real provider succeeds`() {
        val context = boot("app.security.jwt.secret=${UUID.randomUUID()}${UUID.randomUUID()}", "app.billing.provider=abacatepay",
            "app.billing.abacatepay.api-key=x", "app.billing.abacatepay.webhook-secret=x")
        context.close()
    }

    /** C2: startup with the `.env.example` placeholder as JWT_SECRET must fail. */
    @Test
    fun `C2 startup fails when JWT secret is the env example placeholder`() {
        val placeholder = "replace-with-at-least-32-random-bytes-for-local-demo-only"
        var started: ConfigurableApplicationContext? = null
        val failure = runCatching {
            started = boot("app.security.jwt.secret=$placeholder", "app.billing.provider=abacatepay",
                "app.billing.abacatepay.api-key=x", "app.billing.abacatepay.webhook-secret=x")
        }.exceptionOrNull()
        started?.close()
        assertTrue(
            failure != null,
            "Counterexample C2: context STARTED with the .env.example placeholder as JWT secret (length=${placeholder.length})",
        )
        assertTrue(rootMessages(failure).contains("jwt") || rootMessages(failure).contains("secret"), "startup failed for an unrelated reason: ${rootMessages(failure).take(200)}")
        // The failure message must never echo the secret value.
        generateSequence(failure) { it.cause }.forEach { assertFalse(placeholder.isNotEmpty() && (it.message ?: "").contains(placeholder)) }
    }

    /** C2: a secret shorter than 32 bytes must be rejected at startup. */
    @Test
    fun `C2 startup fails when JWT secret is shorter than 32 bytes`() {
        var started: ConfigurableApplicationContext? = null
        val failure = runCatching {
            started = boot("app.security.jwt.secret=short-secret-31-bytes-xxxxxxxxx", "app.billing.provider=abacatepay",
                "app.billing.abacatepay.api-key=x", "app.billing.abacatepay.webhook-secret=x")
        }.exceptionOrNull()
        started?.close()
        assertTrue(failure != null, "Counterexample C2: context STARTED with a 29-byte JWT secret")
        assertTrue(rootMessages(failure).contains("jwt") || rootMessages(failure).contains("secret"), "startup failed for an unrelated reason: ${rootMessages(failure).take(200)}")
    }

    /** C1: outside the `local` profile, an unset PAYMENT_PROVIDER must fail startup (fail-closed). */
    @Test
    fun `C1 startup fails outside local profile when PAYMENT_PROVIDER is unset`() {
        assumeTrue(System.getenv("PAYMENT_PROVIDER") == null, "PAYMENT_PROVIDER is set in the environment")
        val strongSecret = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        var started: ConfigurableApplicationContext? = null
        val failure = runCatching { started = boot("app.security.jwt.secret=$strongSecret") }.exceptionOrNull()
        val provider = started?.environment?.getProperty("app.billing.provider")
        started?.close()
        assertTrue(
            failure != null,
            "Counterexample C1: context STARTED outside 'local' with PAYMENT_PROVIDER unset; effective app.billing.provider='$provider'",
        )
        assertTrue(rootMessages(failure).contains("provider"), "startup failed for an unrelated reason: ${rootMessages(failure).take(200)}")
    }

}
