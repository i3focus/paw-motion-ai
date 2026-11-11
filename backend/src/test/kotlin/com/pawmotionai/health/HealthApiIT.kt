package com.pawmotionai.health

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class HealthApiIT : ApiIntegrationTest() {

    @Test
    fun `health passport records, reminders, weights and vet share links`() {
        val ana = registerOwner("Ana")
        val pet = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val today = LocalDate.now(ZoneOffset.UTC)

        expect(403, call("GET", "/api/pets/$pet/health", bia.token))
        expect(400, call("POST", "/api/pets/$pet/health/records", ana.token, mapOf("type" to "VACCINE", "name" to "Rabies")))
        val rabies = expect(201, call("POST", "/api/pets/$pet/health/records", ana.token, mapOf(
            "type" to "VACCINE", "name" to "Rabies", "administeredOn" to today.minusYears(1).toString(), "dueOn" to today.plusDays(20).toString(),
        )))
        assertEquals("DUE_SOON", rabies["status"].asString())
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM health_reminders WHERE record_id = ?::uuid", Long::class.java, rabies["id"].asString()))
        expect(201, call("POST", "/api/pets/$pet/health/records", ana.token, mapOf(
            "type" to "DEWORMING", "name" to "Deworming", "dueOn" to today.minusDays(3).toString(),
        )))
        expect(200, call("POST", "/api/pets/$pet/weights", ana.token, mapOf("measuredOn" to today.toString(), "weightKg" to 31.5)))
        val weights = expect(200, call("POST", "/api/pets/$pet/weights", ana.token, mapOf("measuredOn" to today.toString(), "weightKg" to 32.0)))
        assertEquals(1, weights.size())

        val passport = expect(200, call("GET", "/api/pets/$pet/health", ana.token))
        assertEquals(listOf("Deworming", "Rabies"), passport["upcoming"].elements().map { it["name"].asString() })
        assertEquals("OVERDUE", passport["upcoming"][0]["status"].asString())

        val updated = expect(200, call("PUT", "/api/pets/$pet/health/records/${rabies["id"].asString()}", ana.token, mapOf(
            "type" to "VACCINE", "name" to "Rabies", "administeredOn" to today.toString(), "dueOn" to today.plusYears(1).toString(),
        )))
        assertEquals("UPCOMING", updated["status"].asString())

        val link = expect(201, call("POST", "/api/pets/$pet/health/share", ana.token, mapOf("days" to 3)))
        val shared = expect(200, call("GET", "/api/vet-share/${link["token"].asString()}"))
        assertEquals("Thor", shared["petName"].asString())
        assertEquals(2, shared["records"].size())
        expect(204, call("DELETE", "/api/pets/$pet/health/share", ana.token))
        expect(404, call("GET", "/api/vet-share/${link["token"].asString()}"))
    }
}
