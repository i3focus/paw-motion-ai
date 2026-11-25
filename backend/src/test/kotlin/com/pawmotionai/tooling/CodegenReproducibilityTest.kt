package com.pawmotionai.tooling

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** C11: codegen must run the exact generator version from the lockfile and CI must fail on generated diff. */
class CodegenReproducibilityTest {

    private val repoRoot: Path = Path.of("..").toAbsolutePath().normalize()
    private fun read(path: String) = Files.readString(repoRoot.resolve(path))

    @Test
    fun `generate script uses the lockfile binary without floating npx`() {
        val script = read("packages/api-client/scripts/generate.sh")
        assertFalse(Regex("npx\\s+(-y|--yes)\\b").containsMatchIn(script), "generate.sh must not use 'npx -y'")
        assertFalse(Regex("openapi-typescript@").containsMatchIn(script), "generate.sh must not pin a version inline")
        assertTrue(Regex("\\bnpm ci\\b").containsMatchIn(script), "generate.sh must install from the lockfile")
        assertTrue(Regex("npx\\s+--no-install\\s+openapi-typescript").containsMatchIn(script), "generate.sh must run the installed binary")
    }

    @Test
    fun `package json pins an exact generator version matching the lockfile`() {
        val pkg = read("packages/api-client/package.json")
        val declared = Regex("\"openapi-typescript\"\\s*:\\s*\"([^\"]+)\"").find(pkg)?.groupValues?.get(1)
        assertTrue(declared != null && Regex("\\d+\\.\\d+\\.\\d+").matches(declared), "version must be exact, was $declared")
        val lock = read("packages/api-client/package-lock.json")
        val locked = Regex("\"node_modules/openapi-typescript\"\\s*:\\s*\\{\\s*\"version\"\\s*:\\s*\"([^\"]+)\"").find(lock)?.groupValues?.get(1)
        assertEquals(declared, locked)
        assertTrue(lock.contains("\"integrity\""), "lockfile must carry integrity hashes")
    }

    @Test
    fun `ci regenerates and fails on diff`() {
        val ci = read(".github/workflows/ci.yml")
        assertTrue(ci.contains("scripts/generate.sh"), "ci.yml must regenerate through generate.sh")
        assertTrue(Regex("git\\s+diff\\s+--exit-code").containsMatchIn(ci), "ci.yml must fail on generated diff")
    }
}
