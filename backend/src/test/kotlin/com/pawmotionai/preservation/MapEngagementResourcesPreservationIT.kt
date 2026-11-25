package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.8 (first lost-pet alert and first playdate response notify), 3.9 (media within quota, paginated
 * lists in the old range, collision-free slug, insight for an own pet, existing media URLs) and 3.10 (plausible
 * activities including the exact limits 86400 s and 500000 m, XP within the cap, impression and first click counted once).
 */
@TestPropertySource(properties = ["app.media.root=build/tmp/preservation-media"])
class MapEngagementResourcesPreservationIT : PreservationApiTest() {

    // ---------------- 3.8 ----------------

    @Test
    fun `first lost-pet alert notifies a nearby owner`() {
        val ana = registerOwner("Ana")
        val neighbor = registerOwner("Nina")
        setLocation(neighbor.ownerId, -23.5616, -46.6560)
        val thor = createPet(ana, "Thor")
        expect(201, call("POST", "/api/lost-pets", ana.token, mapOf(
            "petId" to thor, "description" to "Lost near the park", "latitude" to -23.5615, "longitude" to -46.6559,
            "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 3000,
        )))
        assertEquals(1L, notificationCount(neighbor.ownerId, "Lost pet near you"))
        assertEquals(0L, notificationCount(ana.ownerId, "Lost pet near you"), "the author is not notified")
    }

    @Test
    fun `first playdate response notifies the organizer and the invite notifies the invitee`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        val playdate = expect(201, call("POST", "/api/playdates", ana.token, mapOf(
            "petId" to thor, "mode" to "NOW", "inviteePetIds" to listOf(luna), "message" to "Let's play")))["id"].asString()
        assertEquals(1L, notificationCount(bia.ownerId, "Playdate invite"))

        expect(200, call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")))
        assertEquals(1L, notificationCount(ana.ownerId, "Playdate accepted"))
    }

    // ---------------- 3.9 ----------------

    @Test
    fun `media upload within quota is stored and served at the same URL`() {
        val owner = registerOwner("Ana")
        val upload = expect(201, uploadPng(owner.token, 1))
        val id = upload["id"].asString()
        assertEquals("image/png", upload["contentType"].asString())
        assertTrue(upload["url"].asString().endsWith("/api/media/$id"), "URL shape must stay /api/media/{id}: ${upload["url"].asString()}")
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM media_assets WHERE owner_id = ?", Long::class.java, owner.ownerId))
        // Public download by id keeps working without a token.
        assertEquals(200, call("GET", "/api/media/$id").status)
        assertEquals(404, call("GET", "/api/media/${UUID.randomUUID()}").status)
    }

    @ParameterizedTest(name = "paginated lists in the old range, seed {0}")
    @ValueSource(longs = [1, 2, 3, 4])
    fun `paginated lists answer 200 and echo page and size for small pages`(seed: Long) {
        val random = Random(seed)
        val owner = registerOwner("Ana")
        repeat(3) { page ->
            val size = random.nextInt(1, 51)
            val feed = expect(200, call("GET", "/api/feed?page=$page&size=$size", owner.token))
            assertEquals(page, feed["page"].asInt(), "seed=$seed")
            assertEquals(size, feed["size"].asInt(), "seed=$seed")
        }
    }

    @Test
    fun `wallet entries accept a normal page size`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val accountId = expect(200, call("GET", "/api/partner/wallet", partner.token))["accounts"][0]["id"].asString()
        val page = expect(200, call("GET", "/api/partner/wallet/accounts/$accountId/entries?size=20", partner.token))
        assertEquals(20, page["size"].asInt())
    }

    @Test
    fun `community slug without collision is the plain slug of the name`() {
        val owner = registerOwner("Ana")
        val suffix = UUID.randomUUID().toString().take(8)
        val created = expect(201, call("POST", "/api/communities", owner.token, mapOf("name" to "Plain Club $suffix", "kind" to "BREED")))
        assertEquals("plain-club-$suffix", created["slug"].asString())
    }

    @Test
    fun `insight for an own pet is generated`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        expect(201, recordActivity(owner, pet))
        assertEquals(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7)).status)
    }

    // ---------------- 3.10 ----------------

    @Test
    fun `activity at the exact limits 86400 seconds and 500000 meters is accepted`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        assertEquals(201, recordActivity(owner, pet, durationSeconds = 86_400, distanceMeters = 500_000.0).status)
        assertEquals(201, recordActivity(owner, pet, durationSeconds = 1, distanceMeters = 0.0).status)
    }

    @ParameterizedTest(name = "plausible activities, seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5])
    fun `plausible activities are persisted with the submitted values`(seed: Long) {
        val random = Random(seed)
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(3) {
            val duration = random.nextLong(60, 14_401)
            val distance = random.nextInt(0, 30_001).toDouble()
            val startedAt = Instant.now().minusSeconds(random.nextLong(120, 7 * 24 * 3600))
            val created = expect(201, recordActivity(owner, pet, startedAt = startedAt, durationSeconds = duration, distanceMeters = distance))
            assertEquals(duration, created["durationSeconds"].asLong(), "seed=$seed")
        }
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM activities WHERE pet_id = ?", Long::class.java, pet))
    }

    @Test
    fun `XP within the daily cap is awarded for each plausible activity`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(3) { expect(201, recordActivity(owner, pet, startedAt = Instant.now().minusSeconds(3600L * (it + 1)))) }
        val events = jdbc.queryForObject("SELECT COUNT(*) FROM xp_events WHERE pet_id = ? AND source_type = 'ACTIVITY'", Long::class.java, pet)!!
        val points = jdbc.queryForObject("SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE pet_id = ? AND source_type = 'ACTIVITY'", Long::class.java, pet)!!
        assertEquals(3L, events)
        assertTrue(points > 0)
        assertEquals(points, expect(200, call("GET", "/api/pets/$pet/engagement", owner.token))["xp"].asLong())
    }

    private fun activeCampaign(partner: Session): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO partner_campaigns (id, partner_id, title, description, target_audience, cta, status, created_at, updated_at,
                credits_charged, expires_at, starts_at, ends_at, target_species, formats)
            VALUES (?, ?, 'Spa', 'Spa day', 'dogs', 'Book now', 'ACTIVE', now(), now(), 500,
                now() + interval '30 day', now() - interval '1 day', now() + interval '30 day', '{}', CAST('{FEED_CARD}' AS varchar(32)[]))
            """.trimIndent(),
            id, partner.ownerId,
        )
        return id
    }

    @Test
    fun `impression and first click are each counted once, repeated impression is ignored`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        val viewer = registerOwner("Bia")
        val campaign = activeCampaign(partner)
        repeat(3) { assertEquals(204, call("POST", "/api/ads/$campaign/events", viewer.token, mapOf("type" to "IMPRESSION", "placement" to "FEED_CARD")).status) }
        assertEquals(204, call("POST", "/api/ads/$campaign/events", viewer.token, mapOf("type" to "CLICK", "placement" to "FEED_CARD")).status)
        fun count(type: String) = jdbc.queryForObject("SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ? AND type = ?", Long::class.java, campaign, type)
        assertEquals(1L, count("IMPRESSION"))
        assertEquals(1L, count("CLICK"))
    }
}
