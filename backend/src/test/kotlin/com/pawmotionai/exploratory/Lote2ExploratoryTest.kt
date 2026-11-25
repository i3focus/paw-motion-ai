package com.pawmotionai.exploratory

import com.pawmotionai.configuration.AuthRateLimitFilter
import com.pawmotionai.configuration.WriteRateLimitFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Lote 2 (C17, N13; C16, C25, C26 and C14 moved to configuration/EdgeLimitsTest): edge limits, exercised directly against the filters (follows the model of
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

    // ---------------- C16 / C17 ----------------

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
