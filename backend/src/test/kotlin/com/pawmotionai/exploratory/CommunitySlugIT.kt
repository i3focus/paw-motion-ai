package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 4a (C5): former exploratory test, now regular. Validates: Requirements 2.17, 3.9. */
class CommunitySlugIT : ExploratoryApiTest() {

    @Test
    fun `C5 community creation with many slug collisions runs a bounded number of slug queries`() {
        val creator = registerOwner("Ana")
        val name = "Collision Club ${UUID.randomUUID().toString().take(8)}"
        val base = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val collisions = 12
        (0 until collisions).forEach { i ->
            val slug = if (i == 0) base else "$base-${i + 1}"
            jdbc.update("INSERT INTO communities (id, slug, name, kind, created_by, created_at) VALUES (?, ?, ?, 'NEIGHBORHOOD', ?, now())", UUID.randomUUID(), slug, name, creator.ownerId)
        }
        val mark = startStatementLog()
        try {
            call("POST", "/api/communities", creator.token, mapOf("name" to name, "kind" to "NEIGHBORHOOD"))
            val slugStatements = statementLogSince(mark).count { line ->
                line.contains("execute") && (line.contains("FROM communities WHERE slug") || line.contains("INSERT INTO communities"))
            }
            assertTrue(slugStatements <= ProvisionalLimits.MAX_SLUG_ATTEMPTS,
                "Counterexample C5: $collisions pre-existing slugs -> $slugStatements slug statements for one creation (max ${ProvisionalLimits.MAX_SLUG_ATTEMPTS})")
        } finally {
            stopStatementLog()
        }
    }

    @Test
    fun `C5 concurrent creations with the same name produce distinct slugs and no 5xx`() {
        val creator = registerOwner("Ana")
        val name = "Race Club ${UUID.randomUUID().toString().take(8)}"
        val n = 3
        val pool = Executors.newFixedThreadPool(n)
        val statuses = try {
            pool.invokeAll((1..n).map { Callable { call("POST", "/api/communities", creator.token, mapOf("name" to name, "kind" to "NEIGHBORHOOD")).status } }).map { it.get() }
        } finally {
            pool.shutdown()
        }
        assertTrue(statuses.all { it == 201 || it == 409 }, "unexpected statuses $statuses")
        val slugs = jdbc.queryForList("SELECT slug FROM communities WHERE name = ?", String::class.java, name)
        assertEquals(slugs.size, slugs.toSet().size, "duplicate slugs: $slugs")
        assertEquals(statuses.count { it == 201 }, slugs.size)
    }
}
