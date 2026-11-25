package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Task 11.2 (N7, N8, N9, N2): RSVP capacity under concurrency, RSVP withdrawal and per-owner quotas.
 * Real PostgreSQL via Testcontainers; contention goes through the HTTP API with parallel sessions.
 */
class ConcurrencyQuotasIT : ExploratoryApiTest() {
    private fun <T> parallel(threads: Int, task: (Int) -> T): List<T> {
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until threads).map { i -> pool.submit<T> { start.await(); task(i) } }
            start.countDown()
            return futures.map { it.get(120, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun createCommunity(session: Session, name: String = "Club ${UUID.randomUUID()}") =
        call("POST", "/api/communities", session.token, mapOf("name" to name, "kind" to "CAUSE"))

    private fun communityId(owner: Session): String =
        expect(201, createCommunity(owner))["id"].asString()

    private fun eventBody(startsAt: Instant = Instant.now().plusSeconds(7200), capacity: Int? = null) =
        mapOf("title" to "Meetup", "startsAt" to startsAt.toString(), "capacity" to capacity)

    private fun event(owner: Session, community: String, capacity: Int?): String =
        expect(201, call("POST", "/api/communities/$community/events", owner.token, eventBody(capacity = capacity)))["id"].asString()

    private fun going(eventId: String): Long =
        jdbc.queryForObject("SELECT COUNT(*) FROM event_rsvps WHERE event_id = ?::uuid AND status = 'GOING'", Long::class.java, eventId)!!

    // ---- N7 ----

    @Test
    fun `N7 concurrent GOING never exceeds the event capacity`() {
        val host = registerOwner("Host")
        val eventId = event(host, communityId(host), capacity = 3)
        val guests = (1..16).map { registerOwner("Guest$it") }
        val statuses = parallel(guests.size) { i ->
            call("POST", "/api/events/$eventId/rsvp", guests[i].token, mapOf("status" to "GOING")).status
        }
        assertEquals(3L, going(eventId), "GOING rows must equal capacity, statuses=$statuses")
        assertEquals(3, statuses.count { it == 200 })
        assertEquals(13, statuses.count { it == 409 })
    }

    @Test
    fun `N7 switching INTERESTED to GOING at the last seat is serialized`() {
        val host = registerOwner("Host")
        val eventId = event(host, communityId(host), capacity = 1)
        val guests = (1..8).map { registerOwner("G$it") }
        guests.forEach { expect(200, call("POST", "/api/events/$eventId/rsvp", it.token, mapOf("status" to "INTERESTED"))) }
        parallel(guests.size) { i -> call("POST", "/api/events/$eventId/rsvp", guests[i].token, mapOf("status" to "GOING")).status }
        assertEquals(1L, going(eventId))
    }

    // ---- N8 ----

    @Test
    fun `N8 RSVP can be withdrawn, is idempotent and frees the seat`() {
        val host = registerOwner("Host")
        val eventId = event(host, communityId(host), capacity = 1)
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        // Membership gate unchanged: RSVP does not require community membership (product decision pending).
        expect(200, call("POST", "/api/events/$eventId/rsvp", ana.token, mapOf("status" to "GOING")))
        expect(409, call("POST", "/api/events/$eventId/rsvp", bia.token, mapOf("status" to "GOING")))
        val withdrawn = expect(200, call("DELETE", "/api/events/$eventId/rsvp", ana.token))
        assertEquals(0, withdrawn["goingCount"].asInt())
        assertTrue(withdrawn["viewerRsvp"].isNull)
        expect(200, call("DELETE", "/api/events/$eventId/rsvp", ana.token))
        expect(200, call("POST", "/api/events/$eventId/rsvp", bia.token, mapOf("status" to "GOING")))
        expect(404, call("DELETE", "/api/events/${UUID.randomUUID()}/rsvp", ana.token))
        expect(401, call("DELETE", "/api/events/$eventId/rsvp"))
        // A user can only withdraw their own row.
        expect(200, call("DELETE", "/api/events/$eventId/rsvp", ana.token))
        assertEquals(1L, going(eventId))
    }

    // ---- N9 ----

    @Test
    fun `N9 daily community cap is enforced and serialized per owner`() {
        val owner = registerOwner("Creator")
        val statuses = parallel(10) { createCommunity(owner).status }
        assertEquals(3, statuses.count { it == 201 }, "statuses=$statuses")
        assertEquals(7, statuses.count { it == 409 })
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM communities WHERE created_by = ?", Long::class.java, owner.ownerId))
        // Another owner is unaffected.
        expect(201, createCommunity(registerOwner("Other")))
    }

    @Test
    fun `N9 total communities per owner is capped`() {
        val owner = registerOwner("Collector")
        repeat(5) { i ->
            jdbc.update(
                "INSERT INTO communities (id, slug, name, kind, created_by, created_at) VALUES (?, ?, 'Old', 'CAUSE', ?, now() - interval '3 days')",
                UUID.randomUUID(), "old-$i-${UUID.randomUUID()}", owner.ownerId,
            )
        }
        expect(409, createCommunity(owner))
    }

    @Test
    fun `N9 daily event cap is serialized and active event cap is enforced`() {
        val owner = registerOwner("Organizer")
        val community = communityId(owner)
        val statuses = parallel(16) { call("POST", "/api/communities/$community/events", owner.token, eventBody()).status }
        assertEquals(10, statuses.count { it == 201 }, "statuses=$statuses")
        assertEquals(6, statuses.count { it == 409 })

        val other = registerOwner("Planner")
        val otherCommunity = communityId(other)
        repeat(20) {
            jdbc.update(
                "INSERT INTO community_events (id, community_id, title, starts_at, created_by, created_at) VALUES (?, ?::uuid, 'Old', now() + interval '10 days', ?, now() - interval '3 days')",
                UUID.randomUUID(), otherCommunity, other.ownerId,
            )
        }
        expect(409, call("POST", "/api/communities/$otherCommunity/events", other.token, eventBody()))
        // Past events do not count as active.
        jdbc.update("UPDATE community_events SET starts_at = now() - interval '2 days' WHERE created_by = ? AND id IN (SELECT id FROM community_events WHERE created_by = ? LIMIT 1)", other.ownerId, other.ownerId)
        expect(201, call("POST", "/api/communities/$otherCommunity/events", other.token, eventBody()))
    }

    // ---- N2 ----

    @Test
    fun `N2 daily activity cap per owner is serialized`() {
        val owner = registerOwner("Runner")
        val pet = createPet(owner)
        jdbc.update(
            """
            INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at)
            SELECT gen_random_uuid(), ?, ?, 'WALK', now() - interval '1 hour', 600, 100, now(), now() FROM generate_series(1, 195)
            """.trimIndent(),
            owner.ownerId, pet,
        )
        val statuses = parallel(12) { recordActivity(owner, pet).status }
        assertEquals(5, statuses.count { it == 201 }, "statuses=$statuses")
        assertEquals(7, statuses.count { it == 409 })
        assertEquals(200L, jdbc.queryForObject("SELECT COUNT(*) FROM activities WHERE owner_id = ?", Long::class.java, owner.ownerId))
        // Rows older than a day do not count.
        jdbc.update("UPDATE activities SET created_at = now() - interval '2 days', started_at = now() - interval '2 days' WHERE owner_id = ? AND id IN (SELECT id FROM activities WHERE owner_id = ? LIMIT 50)", owner.ownerId, owner.ownerId)
        expect(201, recordActivity(owner, pet))
    }
}
