package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 3d (C23): sighting caps per alert and per reporter, and the `seenAt` window. Validates: Requirements 2.16, 3.8. */
class LostPetSightingLimitsIT : ExploratoryApiTest() {
    private fun alertBody(petId: UUID) = mapOf(
        "petId" to petId, "description" to "Lost near the park", "latitude" to -23.5615, "longitude" to -46.6559,
        "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 3000,
    )

    private fun sighting(token: String, alert: String, seenAt: Instant = Instant.now()) =
        call("POST", "/api/lost-pets/$alert/sightings", token, mapOf("latitude" to -23.562, "longitude" to -46.652, "seenAt" to seenAt.toString())).status

    private fun newAlert(): String {
        val ana = registerOwner("Ana")
        return expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(createPet(ana, "Thor"))))["id"].asString()
    }

    @Test
    fun `C23 sightings above the per-reporter cap are refused with 409 and others still report`() {
        val alert = newAlert()
        val bia = registerOwner("Bia")
        val statuses = (1..ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER + 3).map { sighting(bia.token, alert) }
        assertEquals(ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER, statuses.count { it == 204 }, "statuses=$statuses")
        assertTrue(statuses.drop(ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER).all { it == 409 }, "statuses=$statuses")
        assertEquals(204, sighting(registerOwner("Caio").token, alert))
    }

    @Test
    fun `C23 sightings above the per-alert cap are refused with 409`() {
        val alert = newAlert()
        val reporters = (1..ProvisionalLimits.MAX_SIGHTINGS_PER_ALERT + 3).map { registerOwner("R$it") }
        val statuses = reporters.map { sighting(it.token, alert) }
        assertEquals(ProvisionalLimits.MAX_SIGHTINGS_PER_ALERT, statuses.count { it == 204 }, "statuses=$statuses")
        assertTrue(statuses.drop(ProvisionalLimits.MAX_SIGHTINGS_PER_ALERT).all { it == 409 }, "statuses=$statuses")
    }

    @Test
    fun `C23 concurrent sightings from one reporter never exceed the cap`() {
        val alert = newAlert()
        val bia = registerOwner("Bia")
        val pool = Executors.newFixedThreadPool(8)
        try {
            val statuses = pool.invokeAll((1..10).map { Callable { sighting(bia.token, alert) } }).map { it.get() }
            assertEquals(ProvisionalLimits.MAX_SIGHTINGS_PER_REPORTER, statuses.count { it == 204 }, "statuses=$statuses")
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `C23 sighting seenAt outside the window is refused and the edges are accepted`() {
        val alert = newAlert()
        val bia = registerOwner("Bia")
        assertEquals(400, sighting(bia.token, alert, Instant.now().minus(400, ChronoUnit.DAYS)))
        assertEquals(400, sighting(bia.token, alert, Instant.now().plus(3, ChronoUnit.DAYS)))
        assertEquals(204, sighting(bia.token, alert, Instant.now().minus(6, ChronoUnit.DAYS)))
    }

    @Test
    fun `C23 sighting on an unknown alert is still 404`() {
        assertEquals(404, sighting(registerOwner("Bia").token, UUID.randomUUID().toString()))
    }
}
