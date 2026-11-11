package com.pawmotionai.petprofile

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetProfileApiIT : ApiIntegrationTest() {

    @Test
    fun `pet profile aggregates stats tabs and friends`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        expect(201, call("POST", "/api/activities", ana.token, mapOf(
            "petId" to thor, "type" to "walk", "startedAt" to Instant.now().toString(),
            "durationSeconds" to 1200, "distanceMeters" to 2000, "latitude" to -23.56, "longitude" to -46.65, "shareToPawCity" to true,
        )))
        val mediaId = UUID.randomUUID()
        jdbc.update("INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (?, ?, ?, 'image/png', 1000, now())", mediaId, ana.ownerId, "test/$mediaId.png")
        expect(201, call("POST", "/api/posts", ana.token, mapOf("caption" to "Good boy", "petId" to thor, "mediaIds" to listOf(mediaId))))

        val viewer = registerOwner("Bia")
        val luna = createPet(viewer, "Luna")
        val profile = expect(200, call("GET", "/api/pets/$thor/profile", viewer.token))
        assertEquals("Thor", profile["pet"]["name"].asString())
        assertTrue(profile["engagement"]["level"].asInt() >= 1)
        assertEquals(0, profile["stats"]["followers"].asInt())
        assertEquals(false, profile["following"].asBoolean())

        val posts = expect(200, call("GET", "/api/pets/$thor/posts", viewer.token))
        assertEquals(1, posts["items"].size())
        val adventures = expect(200, call("GET", "/api/pets/$thor/adventures", viewer.token))
        assertEquals(1, adventures["items"].size())
        assertTrue(expect(200, call("GET", "/api/pets/$thor/badges", viewer.token)).isArray)

        expect(200, call("PUT", "/api/pets/$thor/follow", viewer.token))
        val followed = expect(200, call("GET", "/api/pets/$thor/profile", viewer.token))
        assertEquals(1, followed["stats"]["followers"].asInt())
        assertEquals(true, followed["following"].asBoolean())

        expect(200, call("PUT", "/api/pets/$luna/follow", ana.token))
        val friends = expect(200, call("GET", "/api/pets/$luna/friends", viewer.token))
        assertTrue(friends["items"].any { it["petId"].asString() == thor.toString() })
    }

    @Test
    fun `profile of unknown pet is 404`() {
        val ana = registerOwner("Ana")
        expect(404, call("GET", "/api/pets/${UUID.randomUUID()}/profile", ana.token))
    }
}
