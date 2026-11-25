package com.pawmotionai.exploratory

import com.pawmotionai.configuration.AuthRateLimitFilter
import com.pawmotionai.configuration.WriteRateLimitFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.random.Random

/**
 * Lote 2 (C25, C26, C14, C16, C17, N13): edge limits, exercised directly against the filters (follows the model of
 * AuthRateLimitFilterTest). The filters receive the RAW request URI, like a servlet container delivers it.
 */
@Exploratory
class Lote2ExploratoryTest {

    private val trusted = "127.0.0.1,10.0.0.0/8"

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun request(method: String, rawUri: String, remote: String = "198.51.100.1") =
        MockHttpServletRequest(method, rawUri).apply { remoteAddr = remote }

    private fun send(filter: org.springframework.web.filter.OncePerRequestFilter, method: String, rawUri: String, remote: String = "198.51.100.1"): Int {
        val response = MockHttpServletResponse()
        filter.doFilter(request(method, rawUri, remote), response, MockFilterChain())
        return response.status
    }

    private fun authenticateAs(name: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(name, null, emptyList())
    }

    /** Percent-encodes some letters and appends matrix params, keeping the path semantically equal after normalization. */
    private fun variantOf(path: String, random: Random): String {
        val segments = path.removePrefix("/").split("/")
        val mutated = segments.mapIndexed { index, segment ->
            val encoded = buildString {
                segment.forEachIndexed { i, ch ->
                    val force = index == 1 && i == 0
                    if (ch.isLetter() && (force || random.nextInt(100) < 25)) append("%%%02X".format(ch.code)) else append(ch)
                }
            }
            if (random.nextBoolean()) "$encoded;k=v" else encoded
        }
        return "/" + mutated.joinToString("/")
    }

    // ---------------- C25 ----------------

    @Test
    fun `C25 encoded login path counts against the 10 per minute limit`() {
        val filter = AuthRateLimitFilter(trusted)
        repeat(10) { assertEquals(200, send(filter, "POST", "/api/auth/login")) }
        val status = send(filter, "POST", "/api/auth/log%69n")
        assertEquals(429, status, "Counterexample C25: POST /api/auth/log%69n after 10 canonical attempts -> HTTP $status")
    }

    @ParameterizedTest(name = "C25 seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
    fun `C25 every equivalent encoding of an auth route is limited like the canonical path`(seed: Long) {
        val filter = AuthRateLimitFilter(trusted)
        val variant = variantOf("/api/auth/login", Random(seed))
        repeat(10) { assertEquals(200, send(filter, "POST", "/api/auth/login")) }
        val status = send(filter, "POST", variant)
        assertEquals(429, status, "Counterexample C25 (seed=$seed): $variant -> HTTP $status")
    }

    // ---------------- C26 ----------------

    @Test
    fun `C26 encoded assistant path counts against the AI bucket`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c26")
        val canonical = "/api/assistant/conversations/1/messages"
        repeat(30) { assertEquals(200, send(filter, "POST", canonical)) }
        val variant = "/api/%61ssistant/conversations/1/messages;x=1"
        val status = send(filter, "POST", variant)
        assertEquals(429, status, "Counterexample C26: $variant after 30 AI messages -> HTTP $status")
    }

    @ParameterizedTest(name = "C26 seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5, 6, 7, 8])
    fun `C26 every equivalent encoding of an assistant route is limited like the canonical path`(seed: Long) {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c26-$seed")
        repeat(30) { assertEquals(200, send(filter, "POST", "/api/assistant/conversations/1/messages")) }
        val variant = variantOf("/api/assistant/conversations/1/messages", Random(seed))
        val status = send(filter, "POST", variant)
        assertEquals(429, status, "Counterexample C26 (seed=$seed): $variant -> HTTP $status")
    }

    // ---------------- C14 ----------------

    @Test
    fun `C14 anonymous writes from IP X never throttle IP Y or the payment webhooks`() {
        val filter = WriteRateLimitFilter()
        SecurityContextHolder.clearContext()
        repeat(121) { send(filter, "POST", "/api/auth/login", remote = "203.0.113.7") }
        val otherIp = send(filter, "POST", "/api/auth/login", remote = "198.51.100.9")
        val webhook = send(filter, "POST", "/api/webhooks/payments/abacatepay", remote = "198.51.100.77")
        assertEquals(
            200 to 200, otherIp to webhook,
            "Counterexample C14: after 121 anonymous writes from 203.0.113.7, IP Y got HTTP $otherIp and a webhook got HTTP $webhook",
        )
    }

    // ---------------- C16 / C17 ----------------

    @Test
    fun `C16 abusive GET volume by one account on a costly route is eventually throttled`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c16")
        val statuses = (1..ProvisionalLimits.READ_BURST_PER_MINUTE + 100).map { send(filter, "GET", "/api/feed?page=$it") }
        assertTrue(
            statuses.any { it == 429 },
            "Counterexample C16: ${statuses.size} GET /api/feed in one minute by one account, no 429 (statuses=${statuses.toSet()})",
        )
    }

    @Test
    fun `C17 insights share the 30 per minute AI bucket`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c17")
        val statuses = (1..40).map { send(filter, "POST", "/api/insights") }
        assertEquals(429, statuses.last(), "Counterexample C17: 40 POST /api/insights in one minute -> statuses ${statuses.groupingBy { it }.eachCount()}")
    }

    // ---------------- N13 ----------------

    @Test
    fun `N13 client key is the remote address when the whole forwarded chain is trusted`() {
        val filter = AuthRateLimitFilter(trusted)
        val request = request("POST", "/api/auth/login", remote = "10.1.0.5").apply { addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2") }
        val key = filter.clientKey(request)
        assertEquals("10.1.0.5", key, "Counterexample N13: X-Forwarded-For '10.0.0.1, 10.0.0.2' from trusted 10.1.0.5 yields client key '$key' (client-controlled)")
    }

    @Test
    fun `N13 a non-IP forwarded hop is not resolved through DNS`() {
        val filter = AuthRateLimitFilter(trusted)
        val request = request("POST", "/api/auth/login", remote = "10.1.0.5").apply { addHeader("X-Forwarded-For", "not-an-ip.invalid, 10.0.0.2") }
        val started = System.nanoTime()
        val key = filter.clientKey(request)
        val millis = (System.nanoTime() - started) / 1_000_000
        // Diagnostic: DNS resolution of header text is the hypothesis; the hard assertion is on the key only.
        println("N13 diagnostic: clientKey('not-an-ip.invalid')='$key' took ${millis}ms")
        assertNotEquals("not-an-ip.invalid", key, "Counterexample N13: a header-controlled non-IP string became the rate-limit key")
    }
}
