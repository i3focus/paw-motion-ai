package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lote 3 (C24, C9, C8, C18, C23): notifications and cardinality. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote3ExploratoryIT : ExploratoryApiTest() {

    private fun playdateBody(petId: UUID, invitee: UUID) = mapOf(
        "petId" to petId, "mode" to "NOW", "inviteePetIds" to listOf(invitee), "message" to "Let's play",
    )

    private fun alertBody(petId: UUID) = mapOf(
        "petId" to petId, "description" to "Lost near the park", "latitude" to -23.5615, "longitude" to -46.6559,
        "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 3000,
    )

    @Test
    fun `C9 two identical invites to the same owner within the window notify once`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        repeat(2) { call("POST", "/api/playdates", ana.token, playdateBody(thor, luna)) }
        val rows = notificationCount(bia.ownerId, "Playdate invite")
        assertEquals(1L, rows, "Counterexample C9: 2 identical invites -> $rows invite notifications to the same owner")
    }

    @Test
    fun `C9 an invite to a pet of the same owner does not notify`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val rex = createPet(ana, "Rex")
        expect(201, call("POST", "/api/playdates", ana.token, playdateBody(thor, rex)))
        val rows = notificationCount(ana.ownerId, "Playdate invite")
        assertEquals(0L, rows, "Counterexample C9: organizer notified of an invite to their own pet ($rows)")
    }

    @Test
    fun `C8 two create-resolve cycles respect the fan-out cooldown`() {
        val ana = registerOwner("Ana")
        val neighbor = registerOwner("Nina")
        setLocation(neighbor.ownerId, -23.5616, -46.6560)
        val thor = createPet(ana, "Thor")
        repeat(2) {
            val alert = expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(thor)))["id"].asString()
            expect(200, call("POST", "/api/lost-pets/$alert/resolve", ana.token, mapOf("found" to false)))
        }
        val rows = notificationCount(neighbor.ownerId, "Lost pet near you")
        assertEquals(1L, rows, "Counterexample C8: 2 create/resolve cycles -> $rows fan-out notifications to the same neighbor")
    }

    @Test
    fun `C18 pet creation above the per-owner cap is refused with 409`() {
        val ana = registerOwner("Ana")
        val statuses = (1..ProvisionalLimits.MAX_PETS_PER_OWNER + 5).map {
            call("POST", "/api/pets", ana.token, mapOf("name" to "Pet$it", "species" to "dog", "size" to "small")).status
        }
        val created = statuses.count { it == 201 }
        assertTrue(created <= ProvisionalLimits.MAX_PETS_PER_OWNER && statuses.contains(409),
            "Counterexample C18: created $created pets (cap ${ProvisionalLimits.MAX_PETS_PER_OWNER}), statuses=${statuses.groupingBy { it }.eachCount()}")
    }

    @Test
    fun `C18 active alerts above the per-owner cap are refused with 409`() {
        val ana = registerOwner("Ana")
        val pets = (1..ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER + 2).map { createPet(ana, "Pet$it") }
        val statuses = pets.map { call("POST", "/api/lost-pets", ana.token, alertBody(it)).status }
        val created = statuses.count { it == 201 }
        assertTrue(created <= ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER && statuses.contains(409),
            "Counterexample C18: $created active alerts (cap ${ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER}), statuses=${statuses.groupingBy { it }.eachCount()}")
    }

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
