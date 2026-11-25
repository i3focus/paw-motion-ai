package com.pawmotionai.exploratory

import com.pawmotionai.shared.AiQuota
import com.pawmotionai.shared.QuotaExceededException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 2c (C17, N12): insights consume the monthly AI quota atomically. Validates: Requirements 2.11, 3.7, 3.9. */
class InsightsQuotaIT : ExploratoryApiTest() {

    @Autowired
    private lateinit var aiQuota: AiQuota

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private fun used(ownerId: UUID): Long =
        jdbc.queryForObject("SELECT COALESCE(SUM(messages), 0) FROM ai_usage WHERE owner_id = ?", Long::class.java, ownerId)!!

    @Test
    fun `C17 insight generation consumes the monthly AI quota`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(3) { expect(200, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7))) }
        assertEquals(3L, used(owner.ownerId))
    }

    @Test
    fun `C17 insights beyond the free quota return 429 and are not stored`() {
        val owner = registerOwner("Bia")
        val pet = createPet(owner)
        jdbc.update("INSERT INTO ai_usage (owner_id, period, messages) VALUES (?, to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM'), ?)", owner.ownerId, AiQuota.FREE_MONTHLY_MESSAGES)
        assertEquals(429, call("POST", "/api/insights", owner.token, mapOf("petId" to pet, "periodDays" to 7)).status)
        assertEquals(AiQuota.FREE_MONTHLY_MESSAGES.toLong(), used(owner.ownerId))
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM insights WHERE owner_id = ?", Long::class.java, owner.ownerId))
    }

    @Test
    fun `N12 concurrent consumption never exceeds the free limit`() {
        val owner = registerOwner("Caio")
        val threads = 32
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val tx = TransactionTemplate(transactionManager)
        try {
            val results = (1..threads).map {
                pool.submit<Boolean> {
                    start.await()
                    runCatching { tx.executeWithoutResult { aiQuota.consume(owner.ownerId) } }
                        .map { true }.recover { if (it is QuotaExceededException) false else throw it }.getOrThrow()
                }
            }
            start.countDown()
            val granted = results.count { it.get(60, TimeUnit.SECONDS) }
            assertEquals(AiQuota.FREE_MONTHLY_MESSAGES, granted)
            assertEquals(AiQuota.FREE_MONTHLY_MESSAGES.toLong(), used(owner.ownerId))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `N12 two sessions at the last slot grant exactly one`() {
        val owner = registerOwner("Duda")
        jdbc.update("INSERT INTO ai_usage (owner_id, period, messages) VALUES (?, to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM'), ?)", owner.ownerId, AiQuota.FREE_MONTHLY_MESSAGES - 1)
        val pool = Executors.newFixedThreadPool(2)
        val tx = TransactionTemplate(transactionManager)
        val firstHoldsRow = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        try {
            val first = pool.submit<Boolean> {
                tx.execute {
                    aiQuota.consume(owner.ownerId)
                    firstHoldsRow.countDown()
                    secondStarted.await(10, TimeUnit.SECONDS)
                    Thread.sleep(500) // second session is now blocked on the row lock; commit releases it
                }
                true
            }
            val second = pool.submit<Boolean> {
                firstHoldsRow.await(10, TimeUnit.SECONDS)
                secondStarted.countDown()
                runCatching { tx.executeWithoutResult { aiQuota.consume(owner.ownerId) } }.exceptionOrNull() == null
            }
            assertTrue(first.get(30, TimeUnit.SECONDS))
            assertTrue(!second.get(30, TimeUnit.SECONDS), "second session must be refused after the first took the last slot")
            assertEquals(AiQuota.FREE_MONTHLY_MESSAGES.toLong(), used(owner.ownerId))
        } finally {
            pool.shutdownNow()
        }
    }
}
