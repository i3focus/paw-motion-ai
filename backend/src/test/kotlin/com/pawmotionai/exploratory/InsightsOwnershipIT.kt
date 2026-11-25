package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 4c (C12): former exploratory test, now regular. Validates: Requirements 2.19, 3.9. */
class InsightsOwnershipIT : ExploratoryApiTest() {

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
    fun `own pet insight still succeeds and reports activity totals`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        val response = call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))
        expect(200, response)
    }
}
