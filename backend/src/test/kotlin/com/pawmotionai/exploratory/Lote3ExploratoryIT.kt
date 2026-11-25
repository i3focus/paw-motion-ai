package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lote 3 (C23; C8, C9, C18 and C24 moved to regular tests): notifications and cardinality. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote3ExploratoryIT : ExploratoryApiTest() {

    private fun alertBody(petId: UUID) = mapOf(
        "petId" to petId, "description" to "Lost near the park", "latitude" to -23.5615, "longitude" to -46.6559,
        "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 3000,
    )

    @Test
    fun `C23 sightings above the per-reporter cap are refused`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val alert = expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(createPet(ana, "Thor"))))["id"].asString()
        val statuses = (1..ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER + 4).map {
            call("POST", "/api/lost-pets/$alert/sightings", bia.token, mapOf(
                "latitude" to -23.562, "longitude" to -46.652, "seenAt" to Instant.now().toString(),
            )).status
        }
        val accepted = statuses.count { it == 204 }
        assertTrue(accepted <= ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER,
            "Counterexample C23: $accepted sightings accepted from one reporter (cap ${ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER}), statuses=$statuses")
    }

    @Test
    fun `C23 sighting seenAt outside the window is refused`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val alert = expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(createPet(ana, "Thor"))))["id"].asString()
        val old = call("POST", "/api/lost-pets/$alert/sightings", bia.token, mapOf(
            "latitude" to -23.562, "longitude" to -46.652, "seenAt" to Instant.now().minus(400, ChronoUnit.DAYS).toString())).status
        val future = call("POST", "/api/lost-pets/$alert/sightings", bia.token, mapOf(
            "latitude" to -23.562, "longitude" to -46.652, "seenAt" to Instant.now().plus(3, ChronoUnit.DAYS).toString())).status
        assertEquals(400 to 400, old to future, "Counterexample C23: seenAt 400d ago -> HTTP $old, seenAt +3d -> HTTP $future")
    }
}
