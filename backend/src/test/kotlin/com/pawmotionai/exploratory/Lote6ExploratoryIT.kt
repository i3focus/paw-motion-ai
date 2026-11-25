package com.pawmotionai.exploratory

import com.pawmotionai.identity.application.port.out.PasswordHasher
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Lote 6 (C27, C11): authentication timing oracle and codegen reproducibility. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote6ExploratoryIT : ExploratoryApiTest() {

    @MockitoSpyBean
    private lateinit var passwordHasher: PasswordHasher

    private val repoRoot: Path = Path.of("..").toAbsolutePath().normalize()

    private fun insertAccount(role: String, status: String): String {
        val email = "c27-${UUID.randomUUID()}@example.com"
        jdbc.update(
            "INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at) VALUES (?, ?, 'not-a-real-hash', 'C27', ?, ?, now(), now())",
            UUID.randomUUID(), email, role, status,
        )
        return email
    }

    private fun hashChecks(path: String, email: String): Pair<Int, Int> {
        clearInvocations(passwordHasher)
        val response = call("POST", path, null, mapOf("email" to email, "password" to "Wrong-password-1!"))
        val checks = mockingDetails(passwordHasher).invocations.count { it.method.name == "matches" }
        return response.status to checks
    }

    @Test
    fun `C27 every rejected login path compares a password hash`() {
        val cases = linkedMapOf(
            "owner login, absent account" to hashChecks("/api/auth/login", "absent-${UUID.randomUUID()}@example.com"),
            "owner login, inactive account" to hashChecks("/api/auth/login", insertAccount("OWNER", "DISABLED")),
            "owner login, partner account (wrong partition)" to hashChecks("/api/auth/login", insertAccount("PARTNER", "ACTIVE")),
            "partner login, absent account" to hashChecks("/api/partner/auth/login", "absent-${UUID.randomUUID()}@example.com"),
            "partner login, inactive account" to hashChecks("/api/partner/auth/login", insertAccount("PARTNER", "DISABLED")),
            "partner login, owner account (wrong partition)" to hashChecks("/api/partner/auth/login", insertAccount("OWNER", "ACTIVE")),
        )
        val withoutComparison = cases.filterValues { it.second == 0 }.keys
        assertEquals(emptySet<String>(), withoutComparison, "Counterexample C27: passwordHasher.matches was never called for: $withoutComparison (statuses=${cases.mapValues { it.value.first }})")
        verify(passwordHasher, atLeastOnce()).matches(anyString(), anyString())
    }

    @Test
    fun `C27 rejected logins return the same status for every account class`() {
        val statuses = setOf(
            hashChecks("/api/auth/login", "absent-${UUID.randomUUID()}@example.com").first,
            hashChecks("/api/auth/login", insertAccount("OWNER", "DISABLED")).first,
            hashChecks("/api/auth/login", insertAccount("PARTNER", "ACTIVE")).first,
        )
        assertEquals(1, statuses.size, "Counterexample C27: login rejection statuses differ: $statuses")
    }

    /** Diagnostic only (no hard assertion): prints the timing gap between an absent account and a wrong password. */
    @Test
    fun `C27 timing diagnostic prints elapsed times without asserting`() {
        val registered = "c27-timing-${UUID.randomUUID()}@example.com"
        call("POST", "/api/auth/register", null, mapOf("email" to registered, "password" to "Correct-horse-1!", "displayName" to "Timing"))
        fun median(email: String): Long = (1..3).map {
            val started = System.nanoTime()
            call("POST", "/api/auth/login", null, mapOf("email" to email, "password" to "Wrong-password-1!"))
            (System.nanoTime() - started) / 1_000_000
        }.sorted()[1]
        println("C27 timing diagnostic: absent=${median("absent-${UUID.randomUUID()}@example.com")}ms wrongPassword=${median(registered)}ms")
    }

    @Test
    fun `C11 generate script uses the lockfile binary and CI fails on generated diff`() {
        val script = Files.readString(repoRoot.resolve("packages/api-client/scripts/generate.sh"))
        val ci = Files.readString(repoRoot.resolve(".github/workflows/ci.yml"))
        val findings = buildList {
            if (Regex("npx\\s+(-y|--yes)\\s+openapi-typescript@").containsMatchIn(script)) add("generate.sh runs 'npx -y openapi-typescript@7' (floating version, no lockfile integrity)")
            if (!Regex("git\\s+diff\\s+--exit-code").containsMatchIn(ci)) add("ci.yml has no 'git diff --exit-code' step after codegen")
        }
        assertTrue(findings.isEmpty(), "Counterexample C11: $findings")
        assertFalse(script.contains("@7"), "Counterexample C11: generate.sh pins only a major version")
    }
}
