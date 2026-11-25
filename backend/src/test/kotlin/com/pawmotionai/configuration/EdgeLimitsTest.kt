package com.pawmotionai.configuration

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
 * PR 2a (C25, C26, C14): former exploratory tests, now regular. The filters receive the RAW request URI, like a
 * servlet container delivers it. Validates: Requirements 2.9, 2.10, 3.7.
 */
class EdgeLimitsTest {
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

    // ---------------- C14 (webhook bucket) ----------------
    @Test
    fun `webhook bucket in shadow mode records violations without answering 429`() {
        val registry = SimpleMeterRegistry()
        val filter = WriteRateLimitFilter(ClientIpResolver(trusted), RateLimitSettings(RateLimitSettings.Mode.SHADOW, 5), registry)
        val statuses = (1..8).map { send(filter, "POST", "/api/webhooks/payments/abacatepay", "198.51.100.77") }
        assertEquals(List(8) { 200 }, statuses)
        assertEquals(3.0, registry.counter("ratelimit.shadow_violations", "bucket", "webhook", "route", "webhook").count())
    }

    @Test
    fun `webhook bucket in enforce mode answers 429 only to the exceeding IP and does not use the write bucket`() {
        val registry = SimpleMeterRegistry()
        val filter = WriteRateLimitFilter(ClientIpResolver(trusted), RateLimitSettings(RateLimitSettings.Mode.ENFORCE, 5), registry)
        val statuses = (1..7).map { send(filter, "POST", "/api/webhooks/payments/abacatepay", "198.51.100.77") }
        assertEquals(List(5) { 200 } + listOf(429, 429), statuses)
        assertEquals(2.0, registry.counter("ratelimit.rejected", "bucket", "webhook", "route", "webhook").count())
        assertEquals(200, send(filter, "POST", "/api/webhooks/payments/abacatepay", "198.51.100.78"))
        // Webhook traffic did not consume the anonymous write bucket of the same IP.
        assertEquals(200, send(filter, "POST", "/api/auth/login", "198.51.100.77"))
    }

    // ---------------- RequestPaths / ClientIpResolver ----------------
    @Test
    fun `normalized path decodes, drops matrix params and collapses dot and empty segments`() {
        assertEquals("/api/auth/login", RequestPaths.normalized("/api/auth/log%69n"))
        assertEquals("/api/assistant/conversations/1/messages", RequestPaths.normalized("//api/%61ssistant/./conversations;a=b/1//messages;x=1"))
        assertEquals("/api/auth/login", RequestPaths.normalized("/api/x/../auth/login"))
        assertEquals("/", RequestPaths.normalized(""))
    }

    @Test
    fun `segment prefix does not match look-alike paths`() {
        assertEquals(false, RequestPaths.hasSegmentPrefix("/api/assistants", "/api/assistant"))
        assertEquals(false, RequestPaths.hasSegmentPrefix("/api/pets/api/assistant/x", "/api/assistant"))
        assertEquals(true, RequestPaths.hasSegmentPrefix("/api/assistant/conversations", "/api/assistant"))
    }

    @Test
    fun `client key resolves IPv4 and IPv6 through trusted proxies only`() {
        val resolver = ClientIpResolver("127.0.0.1,::1,10.0.0.0/8")
        assertEquals("203.0.113.8", resolver.clientKey(request("POST", "/x", "10.1.0.5").apply { addHeader("X-Forwarded-For", "203.0.113.8, 10.2.0.4") }))
        assertEquals("2001:db8:0:0:0:0:0:1", resolver.clientKey(request("POST", "/x", "::1").apply { addHeader("X-Forwarded-For", "2001:db8::1") }))
        assertEquals("198.51.100.1", resolver.clientKey(request("POST", "/x", "198.51.100.1").apply { addHeader("X-Forwarded-For", "203.0.113.8") }))
    }


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


    @Test
    fun `C17 insights share the 30 per minute AI bucket`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c17")
        val statuses = (1..40).map { send(filter, "POST", "/api/insights") }
        assertEquals(429, statuses.last(), "40 POST /api/insights in one minute -> ${statuses.groupingBy { it }.eachCount()}")
        assertEquals(30, statuses.count { it != 429 })
    }

    @Test
    fun `C17 assistant and insights draw from the same AI bucket`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("owner-c17-shared")
        repeat(15) { send(filter, "POST", "/api/assistant/conversations/x/messages") }
        val insights = (1..20).map { send(filter, "POST", "/api/insights") }
        assertEquals(15, insights.count { it != 429 })
    }
}
