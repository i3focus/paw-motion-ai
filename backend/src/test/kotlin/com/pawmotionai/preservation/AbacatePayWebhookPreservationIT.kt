package com.pawmotionai.preservation

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.2: with the real provider adapter (AbacatePay) a valid webhook credits exactly once and redelivery
 * does not duplicate. The AbacatePay HTTP API is replaced by a local HTTP server; the adapter, signature check,
 * ledger and idempotency are the real code. The dev API key and the secrets below are test values.
 */
class AbacatePayWebhookPreservationIT : PreservationApiTest() {

    companion object {
        private const val WEBHOOK_SECRET = "preservation-abacate-webhook-secret-not-real"
        // Public HMAC key published by AbacatePay for webhook signatures (same constant as the adapter).
        private const val PUBLIC_KEY =
            "t9dXRhHHo3yDEj5pVDYz0frf7q6bMKyMRmxxCPIPp3RCplBfXRxqlC6ZpiWmOqj4L63qEaeUOtrCI8P0VMUgo6iIga2ri9ogaHFs0WIIywSMg0q7RmBfybe1E5XJcfC4IW3alNqym0tXoAKkzvfEjZxV6bE0oG2zJrNNYmUCKZyV0KZ3JS8Votf9EAWWYdiDkMkpbMdPggfh1EqHlVkMiTady6jOR3hyzGEHrIz2Ret0xHKMbiqkr9HS1JhNHDX9"

        /** externalId (our transaction id) -> status answered by the stubbed provider. */
        private val checkoutStatus = ConcurrentHashMap<String, String>()
        private val checkoutAmountCents = ConcurrentHashMap<String, Long>()

        private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val query = exchange.requestURI.query.orEmpty()
                val body = when {
                    path.endsWith("/checkouts/create") -> {
                        val request = String(exchange.requestBody.readAllBytes(), StandardCharsets.UTF_8)
                        val externalId = Regex("\"externalId\"\\s*:\\s*\"([^\"]+)\"").find(request)!!.groupValues[1]
                        checkoutStatus[externalId] = "PENDING"
                        """{"success":true,"data":{"id":"chk_$externalId","url":"http://127.0.0.1/pay/$externalId"}}"""
                    }
                    path.endsWith("/checkouts/get") -> {
                        val externalId = query.substringAfter("externalId=")
                        val cents = checkoutAmountCents[externalId]
                        val paid = if (cents != null) ""","paidAmount":$cents""" else ""
                        """{"success":true,"data":{"id":"chk_$externalId","externalId":"$externalId","status":"${checkoutStatus[externalId] ?: "PENDING"}"$paid}}"""
                    }
                    else -> """{"success":true,"data":{}}"""
                }.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun provider(registry: DynamicPropertyRegistry) {
            registry.add("app.billing.provider") { "abacatepay" }
            registry.add("app.billing.abacatepay.api-key") { "abc_dev_preservation_not_real" }
            registry.add("app.billing.abacatepay.webhook-secret") { WEBHOOK_SECRET }
            registry.add("app.billing.abacatepay.base-url") { "http://127.0.0.1:${server.address.port}" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }
    }

    private fun sign(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(PUBLIC_KEY.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(body.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun deliver(eventId: String, transactionId: String, signature: String? = null, secret: String = WEBHOOK_SECRET): Response {
        val body = """{"id":"$eventId","event":"checkout.completed","devMode":true,"data":{"id":"chk_$transactionId","externalId":"$transactionId"}}"""
        return rawCall("POST", "/api/webhooks/payments/abacatepay", null, body,
            mapOf("x-webhook-secret" to secret, "x-webhook-signature" to (signature ?: sign(body))))
    }

    @Test
    fun `valid AbacatePay webhook credits once and redelivery does not duplicate`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val created = expect(201, rawCall("POST", "/api/partner/billing/purchases", partner.token, """{"amount":250}""",
            mapOf("Idempotency-Key" to UUID.randomUUID().toString())))
        val transactionId = created["id"].asString()
        assertEquals("abacatepay", created["provider"].asString())
        assertEquals("PENDING", created["status"].asString())

        checkoutStatus[transactionId] = "PAID"
        checkoutAmountCents[transactionId] = 25_000
        val eventId = "evt-${UUID.randomUUID()}"
        assertEquals(200, deliver(eventId, transactionId).status)
        assertEquals(250L, mainBalance(partner))
        assertEquals("CONFIRMED", expect(200, call("GET", "/api/partner/billing/purchases/$transactionId", partner.token))["status"].asString())

        // Same event delivered again, and a new event id for the same transaction: still credited once.
        assertEquals(200, deliver(eventId, transactionId).status)
        assertEquals(200, deliver("evt-${UUID.randomUUID()}", transactionId).status)
        assertEquals(250L, mainBalance(partner))
    }

    @Test
    fun `AbacatePay webhook with an invalid signature or secret is refused and credits nothing`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner, credits = 0)
        val transactionId = expect(201, rawCall("POST", "/api/partner/billing/purchases", partner.token, """{"amount":250}""",
            mapOf("Idempotency-Key" to UUID.randomUUID().toString())))["id"].asString()
        checkoutStatus[transactionId] = "PAID"
        checkoutAmountCents[transactionId] = 25_000

        val badSignature = deliver("evt-${UUID.randomUUID()}", transactionId, signature = "AAAA")
        val badSecret = deliver("evt-${UUID.randomUUID()}", transactionId, secret = "wrong")
        assertTrue(badSignature.status in 400..499, "bad signature answered HTTP ${badSignature.status}")
        assertTrue(badSecret.status in 400..499, "bad secret answered HTTP ${badSecret.status}")
        assertEquals(0L, mainBalance(partner))
    }
}
