package com.pawmotionai.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/** PR 2d (N13): trusted proxy chain and bounded bucket memory. Validates: Requirements 2.12, 3.7. */
class TrustedProxyTest {

    private val trusted = "127.0.0.1,10.0.0.0/8"

    private fun request(remote: String, xff: String?, uri: String = "/api/auth/login") =
        MockHttpServletRequest("POST", uri).apply {
            remoteAddr = remote
            if (xff != null) addHeader("X-Forwarded-For", xff)
        }

    @Test
    fun `fully trusted chain from a trusted peer keys on the peer, not the client-written hop`() {
        val key = ClientIpResolver(trusted).clientKey(request("10.1.0.5", "10.0.0.1, 10.0.0.2"))
        assertEquals("10.1.0.5", key)
    }

    @Test
    fun `non-IP hop is neither resolved nor used as key`() {
        val started = System.nanoTime()
        val key = ClientIpResolver(trusted).clientKey(request("10.1.0.5", "not-an-ip.invalid, 10.0.0.2"))
        assertEquals("10.1.0.5", key)
        assertTrue((System.nanoTime() - started) / 1_000_000 < 500, "literal parse must not hit DNS")
    }

    @Test
    fun `untrusted peer cannot choose its key through X-Forwarded-For`() {
        assertEquals("198.51.100.1", ClientIpResolver(trusted).clientKey(request("198.51.100.1", "203.0.113.9")))
    }

    @Test
    fun `rotating spoofed forwarded values from a trusted proxy cannot evade the auth limit`() {
        val filter = AuthRateLimitFilter(trusted)
        var last = 0
        repeat(12) { index ->
            val response = MockHttpServletResponse()
            filter.doFilter(request("10.1.0.5", "10.0.0.${index + 1}, 10.0.0.200"), response, MockFilterChain())
            last = response.status
        }
        assertEquals(429, last)
    }

    @Test
    fun `real client behind a trusted proxy keeps a distinct key`() {
        assertEquals("203.0.113.8", ClientIpResolver(trusted).clientKey(request("10.1.0.5", "203.0.113.8, 10.2.0.4")))
    }

    @Test
    fun `store never exceeds the ceiling plus shards and still limits overflow keys`() {
        val store = RateLimitStore(windowMillis = 60_000, maxKeys = 100, shards = 8)
        repeat(10_000) { store.hit("k$it", now = 1_000) }
        assertTrue(store.size <= 108, "size=${store.size}")
        assertTrue(store.hit("k-new", now = 1_000) > 1, "overflow keys share counters")
    }

    @Test
    fun `store expires windows and purges them when full`() {
        val store = RateLimitStore(windowMillis = 60_000, maxKeys = 10, shards = 2)
        repeat(10) { store.hit("a$it", now = 1_000) }
        assertEquals(1, store.hit("b", now = 62_000))
        assertTrue(store.size <= 2, "expired keys purged, size=${store.size}")
        assertEquals(1, store.hit("a0", now = 62_000))
    }
}
