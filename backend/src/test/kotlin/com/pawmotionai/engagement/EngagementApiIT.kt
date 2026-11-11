package com.pawmotionai.engagement

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngagementApiIT : ApiIntegrationTest() {

    private fun shareActivity(session: Session, petId: UUID) = expect(201, call("POST", "/api/activities", session.token, mapOf(
        "petId" to petId, "type" to "walk", "startedAt" to Instant.now().toString(),
        "durationSeconds" to 900, "distanceMeters" to 1500, "latitude" to -23.56, "longitude" to -46.65, "shareToPawCity" to true,
    )))["id"].asString()

    @Test
    fun `activity awards xp streak and first badge and publishes achievement feed item`() {
        val ana = registerOwner("Ana")
        val pet = createPet(ana)
        shareActivity(ana, pet)

        val engagement = expect(200, call("GET", "/api/pets/$pet/engagement", ana.token))
        assertTrue(engagement["xp"].asLong() > 0)
        assertTrue(engagement["level"].asInt() >= 1)
        assertEquals(1, engagement["streakDays"].asInt())
        assertTrue(engagement["badgeCount"].asLong() >= 1)

        val badges = expect(200, call("GET", "/api/pets/$pet/badges", ana.token))
        assertTrue(badges.any { it["code"].asString() == "FIRST_WALK" && it["awarded"].asBoolean() })

        val feed = expect(200, call("GET", "/api/feed", ana.token))
        assertTrue(feed["items"].any { it["type"].asString() == "ACHIEVEMENT" })
    }

    @Test
    fun `challenges list join and progress and leaderboard`() {
        val ana = registerOwner("Ana")
        val pet = createPet(ana)
        val challengeId = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO challenges (id, title, description, metric, target_value, xp_reward, starts_on, ends_on, author, created_at)
            VALUES (?, 'Walk week', 'Walk 3 times', 'ACTIVITY_COUNT', 3, 100, CURRENT_DATE - 1, CURRENT_DATE + 6, 'ADMIN', now())
            """.trimIndent(), challengeId,
        )

        val challenges = expect(200, call("GET", "/api/engagement/challenges", ana.token))
        assertEquals("Walk week", challenges[0]["title"].asString())
        assertEquals("ACTIVITY_COUNT", challenges[0]["metric"].asString())

        val joined = expect(200, call("POST", "/api/engagement/challenges/$challengeId/join", ana.token, mapOf("petId" to pet)))
        assertEquals(0, joined["joined"][0]["progress"].asInt())

        shareActivity(ana, pet)
        val after = expect(200, call("GET", "/api/engagement/challenges", ana.token))
        assertEquals(1, after[0]["joined"].first { it["petId"].asString() == pet.toString() }["progress"].asInt())

        val board = expect(200, call("GET", "/api/engagement/leaderboard?scope=global", ana.token))
        assertTrue(board["entries"].any { it["petId"].asString() == pet.toString() })
        expect(400, call("GET", "/api/engagement/leaderboard?scope=planet", ana.token))

        val admin = registerOwner("Boss", role = "ADMIN")
        val body = mapOf(
            "title" to "Sprint", "description" to "Walk 10km", "metric" to "DISTANCE_METERS", "targetValue" to 10000,
            "xpReward" to 250, "startsOn" to LocalDate.now().toString(), "endsOn" to LocalDate.now().plusDays(7).toString(),
        )
        expect(403, call("POST", "/api/admin/challenges", ana.token, body))
        expect(201, call("POST", "/api/admin/challenges", admin.token, body))
    }
}
