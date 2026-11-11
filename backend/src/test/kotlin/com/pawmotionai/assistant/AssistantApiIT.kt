package com.pawmotionai.assistant

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantApiIT : ApiIntegrationTest() {

    @Test
    fun `assistant proposes actions that run only after confirmation`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        jdbc.update("INSERT INTO partner_profiles (partner_id, business_name, category, created_at, updated_at) VALUES (?, 'Happy Spa', 'GROOMING', now(), now())", partner.ownerId)
        val service = UUID.randomUUID()
        jdbc.update("INSERT INTO partner_services (id, partner_id, name, duration_minutes, price_cents, created_at, updated_at) VALUES (?, ?, 'Bath', 60, 8000, now(), now())", service, partner.ownerId)
        val campaign = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO partner_campaigns (id, partner_id, title, description, target_audience, cta, status, created_at, updated_at, credits_charged,
                expires_at, starts_at, ends_at, cta_type, service_id, formats)
            VALUES (?, ?, 'Spa', 'Spa day', 'dogs', 'Book', 'ACTIVE', now(), now(), 100, now() + interval '9 day', now() - interval '1 day',
                now() + interval '9 day', 'BOOKING', ?, '{AI_RECOMMENDATION}')
            """.trimIndent(), campaign, partner.ownerId, service,
        )
        val ana = registerOwner("Ana")
        val pet = createPet(ana, "Thor")
        val bia = registerOwner("Bia")

        val conversation = expect(201, call("POST", "/api/assistant/conversations", ana.token, mapOf("petId" to pet)))["id"].asString()
        expect(404, call("GET", "/api/assistant/conversations/$conversation/messages", bia.token))

        val booking = expect(200, call("POST", "/api/assistant/conversations/$conversation/messages", ana.token, mapOf("content" to "Book a bath for Thor")))
        val action = booking["assistantMessage"]["action"]
        assertEquals("PROPOSED", action["status"].asString())
        assertTrue(action["sponsored"].asBoolean())
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM bookings", Long::class.java))

        expect(403, call("POST", "/api/assistant/actions/${action["id"].asString()}/confirm", bia.token))
        val executed = expect(200, call("POST", "/api/assistant/actions/${action["id"].asString()}/confirm", ana.token))
        assertEquals("EXECUTED", executed["status"].asString())
        assertEquals("AI", jdbc.queryForObject("SELECT source FROM bookings WHERE id = ?::uuid", String::class.java, executed["resultRef"].asString()))
        expect(409, call("POST", "/api/assistant/actions/${action["id"].asString()}/confirm", ana.token))

        val reminder = expect(200, call("POST", "/api/assistant/conversations/$conversation/messages", ana.token, mapOf("content" to "Remind me about the vaccine")))
        val reminderId = reminder["assistantMessage"]["action"]["id"].asString()
        assertEquals("CANCELLED", expect(200, call("POST", "/api/assistant/actions/$reminderId/cancel", ana.token))["status"].asString())
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM health_records WHERE pet_id = ?", Long::class.java, pet))

        assertEquals(4, expect(200, call("GET", "/api/assistant/conversations/$conversation/messages", ana.token)).size())
        assertEquals("Book a bath for Thor", expect(200, call("GET", "/api/assistant/conversations", ana.token))["items"][0]["title"].asString())
    }

    @Test
    fun `free plan monthly quota returns 429`() {
        val ana = registerOwner("Ana")
        val conversation = expect(201, call("POST", "/api/assistant/conversations", ana.token))["id"].asString()
        jdbc.update("INSERT INTO ai_usage (owner_id, period, messages) VALUES (?, to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM'), 30)", ana.ownerId)
        expect(429, call("POST", "/api/assistant/conversations/$conversation/messages", ana.token, mapOf("content" to "hi")))
        jdbc.update("UPDATE owners SET plan = 'PREMIUM' WHERE id = ?", ana.ownerId)
        expect(200, call("POST", "/api/assistant/conversations/$conversation/messages", ana.token, mapOf("content" to "hi")))
    }
}
