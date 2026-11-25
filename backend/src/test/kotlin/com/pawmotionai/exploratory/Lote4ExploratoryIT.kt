package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lote 4 (C5, C10, C12, C13, C16 pagination): data and resources. Spec: security-audit-remediation, task 1. */
@Exploratory
@TestPropertySource(properties = ["app.media.root=build/tmp/exploratory-media"])
class Lote4ExploratoryIT : ExploratoryApiTest() {

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
    fun `C10 concurrent uploads never exceed the per-owner object quota`() {
        val owner = registerOwner("Ana")
        val total = ProvisionalLimits.MAX_MEDIA_OBJECTS_PER_OWNER + 10
        val pool = Executors.newFixedThreadPool(16)
        val statuses = try {
            pool.invokeAll((1..total).map { i -> Callable { uploadPng(owner.token, i).status } }).map { it.get() }
        } finally {
            pool.shutdown()
        }
        val stored = jdbc.queryForObject("SELECT COUNT(*) FROM media_assets WHERE owner_id = ?", Long::class.java, owner.ownerId)!!
        assertTrue(stored <= ProvisionalLimits.MAX_MEDIA_OBJECTS_PER_OWNER,
            "Counterexample C10: $stored media objects stored for one owner (cap ${ProvisionalLimits.MAX_MEDIA_OBJECTS_PER_OWNER}); statuses=${statuses.groupingBy { it }.eachCount()}")
    }

    @Test
    fun `C12 insight for a pet of another owner does not scan activities before the ownership check`() {
        val victim = registerOwner("Vera")
        val attacker = registerOwner("Alex")
        val foreignPet = createPet(victim)
        val mark = startStatementLog()
        try {
            val response = call("POST", "/api/insights", attacker.token, mapOf("petId" to foreignPet, "periodDays" to 90))
            val activityScans = statementLogSince(mark).count { it.contains("execute") && it.contains("from activities", ignoreCase = true) }
            assertTrue(response.status in 400..499, "foreign pet must be refused, got HTTP ${response.status}")
            assertEquals(0, activityScans, "Counterexample C12: foreign-pet insight (HTTP ${response.status}) ran $activityScans activity scans before the ownership check")
        } finally {
            stopStatementLog()
        }
    }

    @Test
    fun `C13 five insight calls on the same day create one row`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(5) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        val rows = jdbc.queryForObject("SELECT COUNT(*) FROM insights WHERE owner_id = ?", Long::class.java, owner.ownerId)
        assertEquals(1L, rows, "Counterexample C13: 5 insight calls on the same day -> $rows rows")
    }

    @Test
    fun `C16 absurd page numbers are rejected with 400`() {
        val owner = registerOwner("Ana")
        val feed = call("GET", "/api/feed?page=100000000&size=50", owner.token).status
        assertEquals(400, feed, "Counterexample C16/N16: GET /api/feed?page=100000000 -> HTTP $feed (offset scan)")
    }

    @Test
    fun `C16 wallet entries page size is limited`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val wallet = expect(200, call("GET", "/api/partner/wallet", partner.token))
        val accountId = wallet["accounts"][0]["id"].asString()
        val size = call("GET", "/api/partner/wallet/accounts/$accountId/entries?size=1000000", partner.token)
        val pageSize = runCatching { json(size)["size"].asInt() }.getOrDefault(-1)
        assertTrue(size.status == 400 || (size.status == 200 && pageSize in 1..50),
            "Counterexample N11: wallet entries size=1000000 -> HTTP ${size.status}, effective size=$pageSize")
    }
}
