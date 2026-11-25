package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/** PR 3a (C24): one response per invite, atomic PENDING -> ACCEPTED/DECLINED. Validates: Requirements 2.15, 3.8. */
class PlaydateOneShotIT : ExploratoryApiTest() {

    private fun setup(): Triple<Session, Session, Pair<UUID, String>> {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        val body = mapOf("petId" to thor, "mode" to "NOW", "inviteePetIds" to listOf(luna), "message" to "Let's play")
        val playdate = expect(201, call("POST", "/api/playdates", ana.token, body))["id"].asString()
        return Triple(ana, bia, luna to playdate)
    }

    @Test
    fun `C24 repeated ACCEPTED responses produce a single notification and 409`() {
        val (ana, bia, ids) = setup()
        val (luna, playdate) = ids
        val statuses = (1..5).map { call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")).status }
        assertEquals(listOf(200, 409, 409, 409, 409), statuses)
        assertEquals(1L, notificationCount(ana.ownerId, "Playdate accepted"))
    }

    @Test
    fun `C24 the answer cannot be changed after the first response`() {
        val (ana, bia, ids) = setup()
        val (luna, playdate) = ids
        expect(200, call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "DECLINED")))
        assertEquals(409, call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")).status)
        assertEquals(0L, notificationCount(ana.ownerId, "Playdate accepted"))
    }

    @Test
    fun `C24 concurrent responses yield one transition and one notification`() {
        val (ana, bia, ids) = setup()
        val (luna, playdate) = ids
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val futures = (1..8).map {
            pool.submit<Int> { start.await(); call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")).status }
        }
        start.countDown()
        val statuses = futures.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()
        assertEquals(1, statuses.count { it == 200 }, "statuses=$statuses")
        assertEquals(7, statuses.count { it == 409 }, "statuses=$statuses")
        assertEquals(1L, notificationCount(ana.ownerId, "Playdate accepted"))
    }

    @Test
    fun `C24 respond to an unknown invite stays 404`() {
        val (_, bia, ids) = setup()
        assertEquals(404, call("POST", "/api/playdates/${UUID.randomUUID()}/respond", bia.token, mapOf("petId" to ids.first, "response" to "ACCEPTED")).status)
    }

    @Test
    fun `only the organizer can cancel and a cancelled playdate refuses responses`() {
        val (ana, bia, ids) = setup()
        val (luna, playdate) = ids
        assertEquals(404, call("POST", "/api/playdates/$playdate/cancel", bia.token, null).status)
        expect(200, call("POST", "/api/playdates/$playdate/cancel", ana.token, null))
        assertEquals(409, call("POST", "/api/playdates/$playdate/cancel", ana.token, null).status)
        assertEquals(409, call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")).status)
    }
}
