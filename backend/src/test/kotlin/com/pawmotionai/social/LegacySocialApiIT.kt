package com.pawmotionai.social

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class LegacySocialApiIT : ApiIntegrationTest() {

    @Test
    fun `shared activity is published to the feed and keeps legacy reaction names`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val petId = createPet(ana)

        val activity = expect(201, call("POST", "/api/activities", ana.token, mapOf(
            "petId" to petId, "type" to "walk", "startedAt" to Instant.now().minusSeconds(600).toString(),
            "durationSeconds" to 600, "distanceMeters" to 1200, "latitude" to -23.55, "longitude" to -46.63,
            "shareToPawCity" to true,
        )))
        val activityId = activity["id"].asString()

        assertEquals(1L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM feed_items WHERE type = 'ACTIVITY' AND ref_id = ?::uuid AND location IS NOT NULL",
            Long::class.java, activityId))

        val reacted = expect(200, call("POST", "/api/activities/$activityId/reactions", bia.token, mapOf("type" to "LOVE")))
        assertEquals("LOVE", reacted["viewerReaction"].asString())
        assertEquals(1, reacted["reactions"]["LOVE"].asInt())
        assertEquals("HEART", jdbc.queryForObject("SELECT type FROM reactions", String::class.java))

        expect(201, call("POST", "/api/activities/$activityId/comments", bia.token, mapOf("body" to "Good boy!")))
        val comments = expect(200, call("GET", "/api/activities/$activityId/comments", ana.token))
        assertEquals("Good boy!", comments[0]["body"].asString())
        assertEquals(activityId, comments[0]["activityId"].asString())

        val feed = expect(200, call("GET", "/api/activities/feed?page=0&size=10", bia.token))
        val post = feed.first { it["id"].asString() == activityId }
        assertEquals(1, post["commentCount"].asInt())
    }
}
