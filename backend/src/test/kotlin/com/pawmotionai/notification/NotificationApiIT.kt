package com.pawmotionai.notification

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationApiIT : ApiIntegrationTest() {

    @Test
    fun `push tokens preferences and notification lifecycle`() {
        val ana = registerOwner("Ana")

        expect(204, call("POST", "/api/devices/push-tokens", ana.token, mapOf("token" to "ExponentPushToken[abc]", "platform" to "IOS")))
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM push_tokens WHERE owner_id = ?::uuid", Long::class.java, ana.ownerId.toString()))
        expect(204, call("DELETE", "/api/devices/push-tokens/ExponentPushToken%5Babc%5D", ana.token))
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM push_tokens WHERE owner_id = ?::uuid", Long::class.java, ana.ownerId.toString()))

        val prefs = expect(200, call("GET", "/api/me/notification-preferences", ana.token))
        assertEquals(true, prefs["playdates"].asBoolean())
        val updated = expect(200, call("PATCH", "/api/me/notification-preferences", ana.token, mapOf("lostPets" to false, "reminders" to false)))
        assertEquals(false, updated["lostPets"].asBoolean())
        assertEquals(true, updated["playdates"].asBoolean())
    }

    @Test
    fun `playdate invite creates notification with read state`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val luna = createPet(bia, "Luna")

        expect(201, call("POST", "/api/playdates", ana.token, mapOf(
            "petId" to thor, "mode" to "SCHEDULED", "startsAt" to Instant.now().plusSeconds(3600).toString(),
            "inviteePetIds" to listOf(luna), "locationName" to "City Park",
        )))

        assertEquals(1, expect(200, call("GET", "/api/notifications/unread-count", bia.token))["unread"].asInt())
        val list = expect(200, call("GET", "/api/notifications", bia.token))
        assertEquals("PLAYDATE", list["items"][0]["type"].asString())
        val notificationId = list["items"][0]["id"].asString()

        expect(204, call("POST", "/api/notifications/$notificationId/read", bia.token))
        assertEquals(0, expect(200, call("GET", "/api/notifications/unread-count", bia.token))["unread"].asInt())
        expect(404, call("POST", "/api/notifications/${UUID.randomUUID()}/read", bia.token))
    }

    @Test
    fun `lost pet alert notifies nearby owners only`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val far = registerOwner("Far")
        jdbc.update("UPDATE owners SET location = ST_SetSRID(ST_MakePoint(-46.651, -23.561), 4326)::geography WHERE id = ?::uuid", bia.ownerId.toString())
        jdbc.update("UPDATE owners SET location = ST_SetSRID(ST_MakePoint(-43.17, -22.90), 4326)::geography WHERE id = ?::uuid", far.ownerId.toString())

        expect(201, call("POST", "/api/lost-pets", ana.token, mapOf(
            "petId" to thor, "description" to "Golden, red collar", "latitude" to -23.56, "longitude" to -46.65,
            "lastSeenAt" to Instant.now().toString(), "radiusMeters" to 3000,
        )))

        val biaList = expect(200, call("GET", "/api/notifications", bia.token))
        assertTrue(biaList["items"].any { it["type"].asString() == "LOST_PET" })
        val farList = expect(200, call("GET", "/api/notifications", far.token))
        assertTrue(farList["items"].none { it["type"].asString() == "LOST_PET" })
    }
}
