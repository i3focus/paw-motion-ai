package com.pawmotionai.preservation

import com.pawmotionai.notification.PostCommitDispatcher
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * N6 (stale/out-of-order webhook redelivery) and N15 (I/O only after commit). Same context properties as
 * [BillingPreservationIT] so the Spring context (and its Postgres connections) is shared.
 */
@ActiveProfiles("local")
@TestPropertySource(properties = [
    "app.billing.provider=fake",
    "app.billing.fake-complete-enabled=true",
    "app.billing.fake-webhook-secret=preservation-fake-webhook-secret-not-real",
])
class PaymentsIoHardeningIT : PreservationApiTest() {
    private val secret = "preservation-fake-webhook-secret-not-real"

    @Autowired private lateinit var dispatcher: PostCommitDispatcher
    @Autowired private lateinit var txManager: PlatformTransactionManager

    private fun purchase(partner: Session, amount: Int): String = expect(201, rawCall(
        "POST", "/api/partner/billing/purchases", partner.token, """{"amount":$amount}""",
        mapOf("Idempotency-Key" to UUID.randomUUID().toString()))).get("id").asString()

    private fun event(id: String, type: String, tx: String) = rawCall(
        "POST", "/api/webhooks/payments/fake", null,
        """{"id":"$id","event":"$type","devMode":true,"data":{"transactionId":"$tx"}}""",
        mapOf("x-fake-webhook-secret" to secret))

    private fun status(partner: Session, tx: String) =
        expect(200, call("GET", "/api/partner/billing/purchases/$tx", partner.token))["status"].asString()

    private fun recorded(id: String) = jdbc.queryForObject(
        "SELECT count(*) FROM payment_webhook_events WHERE provider_event_id = ?", Long::class.java, id)!!

    @Test
    fun `stale failed event after confirmation is acknowledged, recorded and changes nothing`() {
        val partner = registerOwner("Spa", role = "PARTNER"); fundPartner(partner, credits = 0)
        val tx = purchase(partner, 300)
        assertEquals(200, event("evt-${UUID.randomUUID()}", "checkout.completed", tx).status)
        val stale = "evt-${UUID.randomUUID()}"
        assertEquals(200, event(stale, "checkout.failed", tx).status)
        assertEquals(1L, recorded(stale), "stale event must be recorded, not left in a rollback loop")
        assertEquals(200, event(stale, "checkout.failed", tx).status)
        assertEquals(300L, mainBalance(partner))
        assertEquals("CONFIRMED", status(partner, tx))
    }

    @Test
    fun `confirmed state does not regress and a late completed event after refund credits nothing`() {
        val partner = registerOwner("Spa", role = "PARTNER"); fundPartner(partner, credits = 0)
        val tx = purchase(partner, 200)
        assertEquals(200, event("evt-${UUID.randomUUID()}", "checkout.completed", tx).status)
        assertEquals(200, event("evt-${UUID.randomUUID()}", "checkout.refunded", tx).status)
        assertEquals("REFUNDED", status(partner, tx))
        val balanceAfterRefund = mainBalance(partner)
        val late = "evt-${UUID.randomUUID()}"
        assertEquals(200, event(late, "checkout.completed", tx).status)
        assertEquals(1L, recorded(late))
        assertEquals("REFUNDED", status(partner, tx))
        assertEquals(balanceAfterRefund, mainBalance(partner))
    }

    @Test
    fun `concurrent redelivery of the same event credits exactly once`() {
        val partner = registerOwner("Spa", role = "PARTNER"); fundPartner(partner, credits = 0)
        val tx = purchase(partner, 400)
        val id = "evt-${UUID.randomUUID()}"
        val pool = Executors.newFixedThreadPool(6)
        val start = CountDownLatch(1)
        val futures = (1..6).map { pool.submit<Int> { start.await(); event(id, "checkout.completed", tx).status } }
        start.countDown()
        val statuses = futures.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdownNow()
        assertTrue(statuses.all { it == 200 }, "statuses=$statuses")
        assertEquals(400L, mainBalance(partner))
        assertEquals(1L, recorded(id))
        assertEquals("CONFIRMED", status(partner, tx))
    }

    @Test
    fun `oversized event id is rejected as 4xx, never a 500, and credits nothing`() {
        val partner = registerOwner("Spa", role = "PARTNER"); fundPartner(partner, credits = 0)
        val tx = purchase(partner, 100)
        val response = event("e".repeat(161), "checkout.completed", tx)
        assertTrue(response.status in 400..499, "status=${response.status}")
        assertEquals(0L, mainBalance(partner))
    }

    @Test
    fun `post-commit work runs only after commit and is discarded on rollback`() {
        val template = TransactionTemplate(txManager)
        val ran = CountDownLatch(1)
        template.executeWithoutResult {
            dispatcher.afterCommit { ran.countDown() }
            assertEquals(1L, ran.count, "must not run inside the transaction")
        }
        assertTrue(ran.await(5, TimeUnit.SECONDS), "must run after commit")
        val rolledBack = CountDownLatch(1)
        template.executeWithoutResult { status ->
            dispatcher.afterCommit { rolledBack.countDown() }
            status.setRollbackOnly()
        }
        assertFalse(rolledBack.await(500, TimeUnit.MILLISECONDS), "must not run after rollback")
    }
}
