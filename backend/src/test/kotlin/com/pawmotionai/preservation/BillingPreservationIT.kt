package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.1: with the explicit `local` profile and `fake-complete-enabled=true`, the fake purchase flow
 * (create, fake-complete, MAIN credited) and the fake webhook with a valid secret credit exactly once.
 * The properties are set explicitly so the test keeps its meaning after the remediation removes the `fake` defaults.
 */
@ActiveProfiles("local")
@TestPropertySource(properties = [
    "app.billing.provider=fake",
    "app.billing.fake-complete-enabled=true",
    "app.billing.fake-webhook-secret=preservation-fake-webhook-secret-not-real",
])
class BillingPreservationIT : PreservationApiTest() {

    private val webhookSecret = "preservation-fake-webhook-secret-not-real"

    private fun createPurchase(partner: Session, amount: Int): String {
        val created = expect(201, rawCall("POST", "/api/partner/billing/purchases", partner.token, """{"amount":$amount}""",
            mapOf("Idempotency-Key" to UUID.randomUUID().toString())))
        assertEquals("PENDING", created["status"].asString())
        assertEquals("fake", created["provider"].asString())
        return created["id"].asString()
    }

    private fun webhook(eventId: String, transactionId: String, secret: String = webhookSecret) = rawCall(
        "POST", "/api/webhooks/payments/fake", null,
        """{"id":"$eventId","event":"checkout.completed","devMode":true,"data":{"transactionId":"$transactionId"}}""",
        mapOf("x-fake-webhook-secret" to secret),
    )

    @ParameterizedTest(name = "fake-complete credits amount {0}")
    @ValueSource(ints = [10, 137, 5000, 10000])
    fun `fake-complete confirms the purchase and credits MAIN once`(amount: Int) {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val transactionId = createPurchase(partner, amount)

        val completed = expect(200, call("POST", "/api/partner/billing/purchases/$transactionId/fake-complete", partner.token))
        assertEquals("CONFIRMED", completed["status"].asString())
        assertEquals(amount.toLong(), mainBalance(partner))

        // Completing again is idempotent and does not credit twice.
        assertEquals("CONFIRMED", expect(200, call("POST", "/api/partner/billing/purchases/$transactionId/fake-complete", partner.token))["status"].asString())
        assertEquals(amount.toLong(), mainBalance(partner))
    }

    @Test
    fun `fake webhook with a valid secret credits once and redelivery does not duplicate`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val transactionId = createPurchase(partner, 500)

        assertEquals(200, webhook("evt-${UUID.randomUUID()}", transactionId).also { assertEquals(200, it.status) }.status)
        assertEquals(500L, mainBalance(partner))

        val redeliveredId = "evt-${UUID.randomUUID()}"
        assertEquals(200, webhook(redeliveredId, transactionId).status)
        assertEquals(200, webhook(redeliveredId, transactionId).status)
        assertEquals(500L, mainBalance(partner), "redelivery must not credit again")
        assertEquals("CONFIRMED", expect(200, call("GET", "/api/partner/billing/purchases/$transactionId", partner.token))["status"].asString())
    }

    @Test
    fun `fake webhook with a wrong secret is refused and credits nothing`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val transactionId = createPurchase(partner, 500)
        val response = webhook("evt-${UUID.randomUUID()}", transactionId, secret = "wrong-secret")
        assertTrue(response.status in 400..499, "wrong secret answered HTTP ${response.status}")
        assertEquals(0L, mainBalance(partner))
    }
}
