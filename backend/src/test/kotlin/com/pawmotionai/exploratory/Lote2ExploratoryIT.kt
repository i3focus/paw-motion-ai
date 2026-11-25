package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Lote 2 (C17): `/api/insights` must consume the monthly `ai_usage` quota. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote2ExploratoryIT : ExploratoryApiTest() {

    @Test
    fun `C17 insight generation consumes the monthly AI quota`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(3) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        val used = jdbc.queryForObject("SELECT COALESCE(SUM(messages), 0) FROM ai_usage WHERE owner_id = ?", Long::class.java, owner.ownerId)
        assertEquals(3L, used, "Counterexample C17: 3 insights generated, ai_usage.messages=$used")
    }
}
