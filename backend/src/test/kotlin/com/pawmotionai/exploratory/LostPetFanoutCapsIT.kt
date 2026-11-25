package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 3c (C8, C18): fan-out cooldown and per-owner caps for pets and alerts. Validates: Requirements 2.13, 2.16, 3.8. */
@TestPropertySource(properties = ["app.lost-pet.max-alerts-per-day=6"])
class LostPetFanoutCapsIT : ExploratoryApiTest() {
    private fun alertBody(petId: UUID) = mapOf(
        "petId" to petId, "description" to "Lost near the park", "latitude" to -23.5615, "longitude" to -46.6559,
        "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 3000,
    )

    private fun petBody(i: Int) = mapOf("name" to "Pet$i", "species" to "dog", "size" to "small")

    @Test
    fun `C8 first alert notifies the neighbor and a second cycle for the same pet is created without notifying`() {
        val ana = registerOwner("Ana")
        val neighbor = registerOwner("Nina")
        setLocation(neighbor.ownerId, -23.5616, -46.6560)
        val thor = createPet(ana, "Thor")
        val first = expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(thor)))["id"].asString()
        assertEquals(1L, notificationCount(neighbor.ownerId, "Lost pet near you"))
        expect(200, call("POST", "/api/lost-pets/$first/resolve", ana.token, mapOf("found" to false)))
        expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(thor)))
        assertEquals(1L, notificationCount(neighbor.ownerId, "Lost pet near you"))
    }

    @Test
    fun `C8 another pet of the same owner still notifies`() {
        val ana = registerOwner("Ana")
        val neighbor = registerOwner("Nina")
        setLocation(neighbor.ownerId, -23.5616, -46.6560)
        expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(createPet(ana, "Thor"))))
        expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(createPet(ana, "Rex"))))
        assertEquals(2L, notificationCount(neighbor.ownerId, "Lost pet near you"))
    }

    @Test
    fun `C8 alert creation above the daily cap is refused with 409`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        repeat(6) {
            val id = expect(201, call("POST", "/api/lost-pets", ana.token, alertBody(thor)))["id"].asString()
            expect(200, call("POST", "/api/lost-pets/$id/resolve", ana.token, mapOf("found" to false)))
        }
        assertEquals(409, call("POST", "/api/lost-pets", ana.token, alertBody(thor)).status)
    }

    @Test
    fun `C18 pet creation above the per-owner cap is refused with 409`() {
        val ana = registerOwner("Ana")
        val statuses = (1..ProvisionalLimits.MAX_PETS_PER_OWNER + 5).map { call("POST", "/api/pets", ana.token, petBody(it)).status }
        assertEquals(ProvisionalLimits.MAX_PETS_PER_OWNER, statuses.count { it == 201 }, "statuses=$statuses")
        assertTrue(statuses.drop(ProvisionalLimits.MAX_PETS_PER_OWNER).all { it == 409 }, "statuses=$statuses")
    }

    @Test
    fun `C18 concurrent pet creations never pass the cap`() {
        val ana = registerOwner("Ana")
        val pool = Executors.newFixedThreadPool(8)
        try {
            val statuses = pool.invokeAll((1..24).map { i -> Callable { call("POST", "/api/pets", ana.token, petBody(i)).status } }).map { it.get() }
            assertEquals(ProvisionalLimits.MAX_PETS_PER_OWNER, statuses.count { it == 201 }, "statuses=$statuses")
            assertTrue(statuses.all { it == 201 || it == 409 }, "statuses=$statuses")
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `C18 active alerts above the per-owner cap are refused with 409`() {
        val ana = registerOwner("Ana")
        val pets = (1..ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER + 2).map { createPet(ana, "Pet$it") }
        val statuses = pets.map { call("POST", "/api/lost-pets", ana.token, alertBody(it)).status }
        assertEquals(ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER, statuses.count { it == 201 }, "statuses=$statuses")
        assertTrue(statuses.drop(ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER).all { it == 409 }, "statuses=$statuses")
    }

    @Test
    fun `C18 concurrent alert creations never pass the active cap`() {
        val ana = registerOwner("Ana")
        val pets = (1..ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER + 3).map { createPet(ana, "Pet$it") }
        val pool = Executors.newFixedThreadPool(8)
        try {
            val statuses = pool.invokeAll(pets.map { pet -> Callable { call("POST", "/api/lost-pets", ana.token, alertBody(pet)).status } }).map { it.get() }
            assertEquals(ProvisionalLimits.MAX_ACTIVE_ALERTS_PER_OWNER, statuses.count { it == 201 }, "statuses=$statuses")
        } finally {
            pool.shutdown()
        }
    }
}
