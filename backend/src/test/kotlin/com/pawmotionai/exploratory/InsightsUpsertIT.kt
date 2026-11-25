package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** PR 4d (C13): former exploratory test, now regular. Validates: Requirements 2.19, 3.9, 3.13. */
class InsightsUpsertIT : ExploratoryApiTest() {

    @Test
    fun `C13 five insight calls on the same day create one row`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(5) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        val rows = jdbc.queryForObject("SELECT COUNT(*) FROM insights WHERE owner_id = ?", Long::class.java, owner.ownerId)
        assertEquals(1L, rows, "Counterexample C13: 5 insight calls on the same day -> $rows rows")
    }

    @Test
    fun `legacy rows without bucket_date are preserved`() {
        val owner = registerOwner("Bia")
        val pet = createPet(owner)
        jdbc.update(
            "INSERT INTO insights (id, owner_id, pet_id, text, provider, fallback_used, generated_at, created_at, updated_at) " +
                "VALUES (gen_random_uuid(), ?, ?, 'old', 'p', false, now(), now(), now())", owner.ownerId, pet)
        repeat(2) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        val rows = jdbc.queryForObject("SELECT COUNT(*) FROM insights WHERE owner_id = ?", Long::class.java, owner.ownerId)
        assertEquals(2L, rows)
    }
}
