package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.3 (login and token use with a strong secret, web and mobile), 3.4 (legitimate admin) and 3.11
 * (correct login authenticates, wrong password answers the same status and body for every account class).
 * Own Spring context (the property below) so the auth rate limiter budget (10 per route per minute) is not shared.
 */
@TestPropertySource(properties = ["preservation.suite=auth"])
class AuthPreservationIT : PreservationApiTest() {

    private val password = "Correct-horse-1!"

    private fun register(path: String, body: Map<String, Any?>) = expect(201, call("POST", path, null, body))

    private fun insertAccount(role: String, status: String): String {
        val email = "pres-${UUID.randomUUID()}@example.com"
        jdbc.update(
            "INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at) VALUES (?, ?, 'not-a-real-hash', 'Pres', ?, ?, now(), now())",
            UUID.randomUUID(), email, role, status,
        )
        return email
    }

    @Test
    fun `owner registers, logs in and the token opens owner routes`() {
        val email = "owner-${UUID.randomUUID()}@example.com"
        val registered = register("/api/auth/register", mapOf("email" to email, "password" to password, "displayName" to "Ana"))
        assertTrue(registered["accessToken"].asString().isNotBlank())

        val login = expect(200, call("POST", "/api/auth/login", null, mapOf("email" to email, "password" to password)))
        assertEquals("Bearer", login["tokenType"].asString())
        assertTrue(login["expiresInSeconds"].asLong() > 0)
        val token = login["accessToken"].asString()
        assertEquals(email, expect(200, call("GET", "/api/me", token))["email"].asString())
        assertEquals(200, call("GET", "/api/pets", token).status)
        assertEquals(401, call("GET", "/api/pets", null).status)
        // An owner token never opens partner or admin routes.
        assertEquals(403, call("GET", "/api/partner/billing/pricing", token).status)
        assertEquals(403, call("GET", "/api/admin/campaigns", token).status)
    }

    @Test
    fun `partner registers, logs in and the token opens partner routes only`() {
        val email = "partner-${UUID.randomUUID()}@example.com"
        register("/api/partner/auth/register", mapOf(
            "email" to email, "password" to password, "displayName" to "Spa", "businessName" to "Happy Spa"))
        val login = expect(200, call("POST", "/api/partner/auth/login", null, mapOf("email" to email, "password" to password)))
        val token = login["accessToken"].asString()
        assertEquals(200, call("GET", "/api/partner/billing/pricing", token).status)
        assertEquals(403, call("GET", "/api/admin/campaigns", token).status)
    }

    @Test
    fun `wrong password answers the same status and body for absent, inactive, wrong-partition and wrong-password accounts`() {
        val email = "known-${UUID.randomUUID()}@example.com"
        register("/api/auth/register", mapOf("email" to email, "password" to password, "displayName" to "Known"))
        val responses = linkedMapOf(
            "wrong password" to call("POST", "/api/auth/login", null, mapOf("email" to email, "password" to "Wrong-password-1!")),
            "absent account" to call("POST", "/api/auth/login", null, mapOf("email" to "absent-${UUID.randomUUID()}@example.com", "password" to "Wrong-password-1!")),
            "inactive account" to call("POST", "/api/auth/login", null, mapOf("email" to insertAccount("OWNER", "DISABLED"), "password" to "Wrong-password-1!")),
            "partner on owner login" to call("POST", "/api/auth/login", null, mapOf("email" to insertAccount("PARTNER", "ACTIVE"), "password" to "Wrong-password-1!")),
        )
        assertEquals(401, responses.getValue("wrong password").status)
        val distinct = responses.mapValues { (_, r) -> r.status to normalize(r.body) }.values.toSet()
        assertEquals(1, distinct.size, "status/body differ across account classes: $responses")
    }

    @Test
    fun `wrong password on partner login answers the same status and body as an absent partner`() {
        val email = "partner-${UUID.randomUUID()}@example.com"
        register("/api/partner/auth/register", mapOf("email" to email, "password" to password, "displayName" to "Spa", "businessName" to "Happy Spa"))
        val wrong = call("POST", "/api/partner/auth/login", null, mapOf("email" to email, "password" to "Wrong-password-1!"))
        val absent = call("POST", "/api/partner/auth/login", null, mapOf("email" to "absent-${UUID.randomUUID()}@example.com", "password" to "Wrong-password-1!"))
        assertEquals(401, wrong.status)
        assertEquals(wrong.status to normalize(wrong.body), absent.status to normalize(absent.body))
    }

    @Test
    fun `database admin reaches admin routes while owner and partner do not`() {
        val admin = registerOwner("Boss", role = "ADMIN")
        val owner = registerOwner("Ana")
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)

        assertEquals(200, call("GET", "/api/admin/campaigns", admin.token).status)
        assertEquals(403, call("GET", "/api/admin/campaigns", owner.token).status)
        assertEquals(403, call("GET", "/api/admin/campaigns", partner.token).status)
        // Wallet admin: unblocking an active wallet is allowed for admins only.
        assertEquals(200, call("POST", "/api/admin/partners/${partner.ownerId}/wallet/unblock", admin.token).status)
        assertEquals(403, call("POST", "/api/admin/partners/${partner.ownerId}/wallet/unblock", partner.token).status)
        // Moderation: admin verifies a partner.
        assertEquals(200, call("POST", "/api/admin/partners/${partner.ownerId}/verify", admin.token).status)
        assertNotEquals(401, call("GET", "/api/admin/campaigns?status=IN_REVIEW", admin.token).status)
    }

    /** Drops fields that legitimately vary per request (timestamps, request ids). */
    private fun normalize(body: String): String = body.replace(Regex("\"(timestamp|traceId|requestId)\"\\s*:\\s*\"[^\"]*\""), "")
}
