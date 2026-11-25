package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lote 4 (C13; C12 moved to InsightsOwnershipIT; C10 moved to MediaQuotaIT; C5 moved to CommunitySlugIT; C16 moved to ReadLimitsIT): data and resources. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote4ExploratoryIT : ExploratoryApiTest() {

    @Test
    fun `C13 five insight calls on the same day create one row`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(5) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        val rows = jdbc.queryForObject("SELECT COUNT(*) FROM insights WHERE owner_id = ?", Long::class.java, owner.ownerId)
        assertEquals(1L, rows, "Counterexample C13: 5 insight calls on the same day -> $rows rows")
    }
}
