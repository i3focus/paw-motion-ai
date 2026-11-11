package com.pawmotionai.booking

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals

class BookingApiIT : ApiIntegrationTest() {

    @Test
    fun `partner services, booking lifecycle, slot conflicts and role checks`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        jdbc.update("INSERT INTO partner_profiles (partner_id, business_name, category, created_at, updated_at) VALUES (?, 'Happy Spa', 'GROOMING', now(), now())", partner.ownerId)
        val ana = registerOwner("Ana")
        val pet = createPet(ana, "Thor")
        val bia = registerOwner("Bia")

        expect(403, call("POST", "/api/partner/services", ana.token, mapOf("name" to "Bath", "durationMinutes" to 60, "priceCents" to 8000)))
        val bath = expect(201, call("POST", "/api/partner/services", partner.token, mapOf("name" to "Bath", "durationMinutes" to 60, "priceCents" to 8000)))
        assertEquals(1, expect(200, call("GET", "/api/partners/${partner.ownerId}", ana.token))["services"].size())

        val start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS)
        val body = mapOf("serviceId" to bath["id"].asString(), "petId" to pet, "startsAt" to start.toString())
        expect(400, call("POST", "/api/bookings", ana.token, body + ("startsAt" to Instant.now().toString())))
        expect(403, call("POST", "/api/bookings", bia.token, body))
        val booking = expect(201, call("POST", "/api/bookings", ana.token, body))
        assertEquals("REQUESTED", booking["status"].asString())
        assertEquals("Happy Spa", booking["partnerName"].asString())
        expect(409, call("POST", "/api/bookings", ana.token, body + ("startsAt" to start.plus(30, ChronoUnit.MINUTES).toString())))
        expect(201, call("POST", "/api/bookings", ana.token, body + ("startsAt" to start.plus(60, ChronoUnit.MINUTES).toString())))

        val id = booking["id"].asString()
        expect(409, call("POST", "/api/partner/bookings/$id/complete", partner.token))
        assertEquals("CONFIRMED", expect(200, call("POST", "/api/partner/bookings/$id/confirm", partner.token))["status"].asString())
        expect(403, call("POST", "/api/bookings/$id/cancel", bia.token))
        assertEquals("CANCELLED", expect(200, call("POST", "/api/bookings/$id/cancel", ana.token))["status"].asString())
        assertEquals(2, expect(200, call("GET", "/api/partner/bookings", partner.token))["items"].size())
        assertEquals(2, expect(200, call("GET", "/api/bookings", ana.token))["items"].size())
        expect(404, call("POST", "/api/bookings", ana.token, body + ("serviceId" to UUID.randomUUID().toString())))
    }
}
