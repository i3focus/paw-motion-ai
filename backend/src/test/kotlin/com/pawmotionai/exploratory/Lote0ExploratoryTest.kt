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
 * Lote 0 (C1, C2, C20): operational containment. Boot-level and script-level checks that do not need an HTTP server.
 * Spec: security-audit-remediation, tasks 1 (Lote 0).
 */
@Exploratory
class Lote0ExploratoryTest {

    private val repoRoot: Path = Path.of("..").toAbsolutePath().normalize()

    private fun envExampleValue(key: String): String =
        Files.readAllLines(repoRoot.resolve(".env.example")).firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")?.trim() ?: ""

    /** C2: the literal shipped in `.env.example` must never be an acceptable secret (nor even be present). */
    @Test
    fun `C2 env example does not ship a usable JWT secret`() {
        val value = envExampleValue("JWT_SECRET")
        assertTrue(
            value.isEmpty(),
            "Counterexample C2: .env.example ships a usable JWT_SECRET placeholder (length=${value.length}, prefix='${value.take(13)}')",
        )
    }

    /** C20: seed-demo.sh must refuse to run without explicit local mode, before touching the network. */
    @Test
    fun `C20 seed-demo script aborts on a remote target without explicit opt-in`() {
        val script = repoRoot.resolve("scripts/seed-demo.sh")
        val process = ProcessBuilder("bash", script.toString())
            .redirectErrorStream(true)
            .apply {
                environment().clear()
                environment()["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"
                environment()["HOME"] = System.getProperty("java.io.tmpdir")
                environment()["API_BASE_URL"] = "http://seed-target.invalid" // never resolves, so nothing can be seeded
                environment()["DB_CONTAINER"] = "exploratory-nonexistent-container"
                environment()["OWNER_COUNT"] = "3"
            }.start()
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(finished, "script did not terminate")
        assertNotEquals(0, process.exitValue())
        assertTrue(
            output.contains("SEED_ALLOW", ignoreCase = true),
            "Counterexample C20: script did not stop on a guard for SEED_ALLOW_*; it went on to the network phase. Output tail: ${output.takeLast(300)}",
        )
    }

    /** C20: no published seed password and no database password fallback in the script. */
    @Test
    fun `C20 seed-demo script has no published password or database fallback`() {
        val text = Files.readString(repoRoot.resolve("scripts/seed-demo.sh"))
        val findings = buildList {
            if (text.contains("PetLife123!")) add("constant seed password 'PetLife123!'")
            if (text.contains("devpw-local-only")) add("database password fallback 'devpw-local-only-*'")
            if (!text.contains("SEED_ALLOW_LOCAL")) add("no SEED_ALLOW_LOCAL guard")
        }
        assertEquals(emptyList<String>(), findings, "Counterexample C20")
    }

    /** C20: dev-up.sh must not copy a usable secret into `.env`. */
    @Test
    fun `C20 dev-up script does not copy env example verbatim`() {
        val text = Files.readString(repoRoot.resolve("scripts/dev-up.sh"))
        assertFalse(text.contains("cp .env.example .env"), "Counterexample C20: dev-up.sh copies .env.example (with its placeholder secret) to .env")
    }
}

/** C1: `fake-complete` must not credit outside the explicit local profile. */
class Lote0FakeCompleteExploratoryIT : ExploratoryApiTest() {

    @Test
    fun `C1 fake-complete returns 404 and credits nothing outside the local profile`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val created = expect(201, rawJsonCall("POST", "/api/partner/billing/purchases", partner.token, """{"amount":10000}""", mapOf("Idempotency-Key" to UUID.randomUUID().toString())))
        val transactionId = created["id"].asString()

        val response = call("POST", "/api/partner/billing/purchases/$transactionId/fake-complete", partner.token)
        val credited = jdbc.queryForObject(
            """
            SELECT COALESCE(SUM(a.balance), 0) FROM ledger_accounts a
            JOIN ledger_wallets w ON w.id = a.wallet_id WHERE w.partner_id = ?
            """.trimIndent(),
            Long::class.java, partner.ownerId,
        )
        assertEquals(
            404 to 0L, response.status to credited,
            "Counterexample C1: fake-complete answered ${response.status} and the wallet holds $credited credits (profiles=${environment.activeProfiles.toList()})",
        )
    }
}
