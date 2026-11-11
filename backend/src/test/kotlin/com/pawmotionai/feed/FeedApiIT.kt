package com.pawmotionai.feed

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedApiIT : ApiIntegrationTest() {

    private fun shareActivity(session: Session, petId: UUID, minutesAgo: Long) = expect(201, call("POST", "/api/activities", session.token, mapOf(
        "petId" to petId, "type" to "walk", "startedAt" to Instant.now().minusSeconds(minutesAgo * 60).toString(),
        "durationSeconds" to 900, "distanceMeters" to 1500, "latitude" to -23.56, "longitude" to -46.65, "shareToPawCity" to true,
    )))["id"].asString()

    private fun activeCampaign(partner: Session, title: String, species: String = "{}") {
        jdbc.update(
            """
            INSERT INTO partner_campaigns (id, partner_id, title, description, target_audience, cta, status, created_at, updated_at,
                credits_charged, expires_at, starts_at, ends_at, target_species, formats)
            VALUES (?, ?, ?, 'Spa day for your pet', 'dogs', 'Book now', 'ACTIVE', now(), now(), 500,
                now() + interval '30 day', now() - interval '1 day', now() + interval '30 day', CAST(? AS varchar(32)[]), '{FEED_CARD}')
            """.trimIndent(),
            UUID.randomUUID(), partner.ownerId, title, species,
        )
    }

    @Test
    fun `feed pages interleave sponsored cards and typed reactions round-trip`() {
        val ana = registerOwner("Ana")
        val petId = createPet(ana)
        repeat(12) { shareActivity(ana, petId, it.toLong() + 1) }
        val partner = registerOwner("Spa", role = "PARTNER")
        activeCampaign(partner, "Spa A")
        activeCampaign(partner, "Spa B")
        activeCampaign(partner, "Cats only", species = "{CAT}")

        val viewer = registerOwner("Bia")
        val page = expect(200, call("GET", "/api/feed?size=10", viewer.token))
        val types = page["items"].elements().map { it["type"].asString() }

        assertEquals(10, types.count { it == "ACTIVITY" })
        assertEquals(2, types.count { it == "SPONSORED" })
        assertTrue(types.first() != "SPONSORED")
        types.zipWithNext().forEach { (a, b) -> assertTrue(a != "SPONSORED" || b != "SPONSORED") }
        val sponsored = page["items"].elements().filter { it["type"].asString() == "SPONSORED" }
        assertTrue(sponsored.all { it["sponsored"]["label"].asString() == "Sponsored" })
        assertTrue(sponsored.none { it["sponsored"]["title"].asString() == "Cats only" })
        assertTrue(page["hasNext"].asBoolean())

        val itemId = page["items"][0]["id"].asString()
        val reacted = expect(200, call("PUT", "/api/feed/$itemId/reaction", viewer.token, mapOf("type" to "LAUGH")))
        assertEquals("LAUGH", reacted["viewerReaction"].asString())
        assertEquals(1, reacted["counts"]["LAUGH"].asInt())
        val cleared = expect(200, call("DELETE", "/api/feed/$itemId/reaction", viewer.token))
        assertEquals(0, cleared["counts"]["LAUGH"].asInt())

        expect(201, call("POST", "/api/feed/$itemId/comments", viewer.token, mapOf("body" to "  So cute!  ")))
        val comments = expect(200, call("GET", "/api/feed/$itemId/comments", viewer.token))
        assertEquals("So cute!", comments["items"][0]["body"].asString())

        val campaignId = sponsored[0]["sponsored"]["campaignId"].asString()
        repeat(2) { expect(204, call("POST", "/api/ads/$campaignId/events", viewer.token, mapOf("type" to "IMPRESSION", "placement" to "FEED_CARD"))) }
        expect(204, call("POST", "/api/ads/$campaignId/events", viewer.token, mapOf("type" to "CLICK", "placement" to "FEED_CARD")))
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ?::uuid", Long::class.java, campaignId))
    }

    @Test
    fun `feed rejects invalid input`() {
        val viewer = registerOwner("Caio")
        expect(400, call("GET", "/api/feed?lat=10", viewer.token))
        expect(404, call("PUT", "/api/feed/${UUID.randomUUID()}/reaction", viewer.token, mapOf("type" to "PAW")))
        expect(422, call("PUT", "/api/feed/${UUID.randomUUID()}/reaction", viewer.token, mapOf<String, Any>()))
        expect(401, call("GET", "/api/feed"))
    }
}
