package com.pawmotionai.map

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapApiIT : ApiIntegrationTest() {

    private fun place(name: String, category: String, lat: Double, lng: Double): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO places (id, name, category, location, created_at, updated_at) VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, now(), now())",
            id, name, category, lng, lat,
        )
        return id
    }

    @Test
    fun `places are filtered by radius and category and sorted by distance`() {
        val ana = registerOwner("Ana")
        val pet = createPet(ana)
        val park = place("Ibirapuera Park", "PARK", -23.5874, -46.6576)
        place("Villa-Lobos Park", "PARK", -23.5466, -46.7230)
        place("Vet Center", "VET", -23.5880, -46.6580)
        place("Far Park", "PARK", -22.9068, -43.1729)

        val parks = expect(200, call("GET", "/api/places?lat=-23.5870&lng=-46.6570&radius=10000&category=PARK", ana.token))
        assertEquals(listOf("Ibirapuera Park", "Villa-Lobos Park"), parks["items"].elements().map { it["name"].asString() })
        val all = expect(200, call("GET", "/api/places?lat=-23.5870&lng=-46.6570&radius=2000", ana.token))
        assertEquals(2, all["items"].size())
        val search = expect(200, call("GET", "/api/places?lat=-23.5870&lng=-46.6570&radius=50000&q=vet", ana.token))
        assertEquals("Vet Center", search["items"][0]["name"].asString())
        expect(400, call("GET", "/api/places?lat=-123&lng=0", ana.token))

        expect(201, call("POST", "/api/places/$park/check-ins", ana.token, mapOf("petId" to pet)))
        expect(409, call("POST", "/api/places/$park/check-ins", ana.token, mapOf("petId" to pet)))
        assertEquals(1, expect(200, call("GET", "/api/places/$park", ana.token))["checkInsToday"].asInt())
        val bia = registerOwner("Bia")
        expect(403, call("POST", "/api/places/$park/check-ins", bia.token, mapOf("petId" to pet)))
    }

    @Test
    fun `lost pet alert lifecycle`() {
        val ana = registerOwner("Ana")
        val pet = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val body = mapOf(
            "petId" to pet, "description" to "Golden, red collar", "latitude" to -23.56, "longitude" to -46.65,
            "lastSeenAt" to Instant.now().minusSeconds(600).toString(), "radiusMeters" to 2000,
        )
        expect(403, call("POST", "/api/lost-pets", bia.token, body))
        val alert = expect(201, call("POST", "/api/lost-pets", ana.token, body))
        expect(409, call("POST", "/api/lost-pets", ana.token, body))

        val nearby = expect(200, call("GET", "/api/lost-pets?lat=-23.561&lng=-46.651", bia.token))
        assertEquals("Thor", nearby[0]["petName"].asString())
        assertEquals(0, expect(200, call("GET", "/api/lost-pets?lat=-22.90&lng=-43.17", bia.token)).size())

        val alertId = alert["id"].asString()
        expect(204, call("POST", "/api/lost-pets/$alertId/sightings", bia.token, mapOf("latitude" to -23.562, "longitude" to -46.652, "seenAt" to Instant.now().toString())))
        expect(403, call("POST", "/api/lost-pets/$alertId/resolve", bia.token, mapOf("found" to true)))
        val resolved = expect(200, call("POST", "/api/lost-pets/$alertId/resolve", ana.token, mapOf("found" to true)))
        assertEquals("FOUND", resolved["status"].asString())
        assertEquals(1, resolved["sightings"].asInt())
    }

    @Test
    fun `playdate invites can be answered only by the invited owner`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val luna = createPet(bia, "Luna")
        val park = place("City Park", "PARK", -15.7939, -47.8828)

        expect(400, call("POST", "/api/playdates", ana.token, mapOf("petId" to thor, "mode" to "SCHEDULED", "placeId" to park)))
        val playdate = expect(201, call("POST", "/api/playdates", ana.token, mapOf(
            "petId" to thor, "mode" to "SCHEDULED", "placeId" to park, "startsAt" to Instant.now().plusSeconds(3600).toString(),
            "inviteePetIds" to listOf(luna),
        )))
        assertEquals("City Park", playdate["locationName"].asString())
        val id = playdate["id"].asString()
        expect(404, call("POST", "/api/playdates/$id/respond", ana.token, mapOf("petId" to luna, "response" to "ACCEPTED")))
        val answered = expect(200, call("POST", "/api/playdates/$id/respond", bia.token, mapOf("petId" to luna, "response" to "ACCEPTED")))
        assertEquals("ACCEPTED", answered["invites"][0]["status"].asString())
        assertTrue(expect(200, call("GET", "/api/playdates", bia.token)).size() == 1)
    }
}
