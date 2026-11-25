package com.pawmotionai.configuration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/** PR 2b (C16): read bucket per (subject, route). Validates: Requirements 2.11, 3.7. */
class ReadLimitsTest {
    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun authenticateAs(name: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(name, null, emptyList())
    }

    private fun get(filter: ReadRateLimitFilter, uri: String): Int {
        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest("GET", uri).apply { remoteAddr = "198.51.100.1" }, response, MockFilterChain())
        return response.status
    }

    @Test
    fun `abusive GET volume by one account on a costly route is throttled`() {
        val filter = ReadRateLimitFilter()
        authenticateAs("owner-c16")
        val statuses = (1..1_100).map { get(filter, "/api/feed") }
        assertTrue(statuses.any { it == 429 })
    }

    @Test
    fun `legitimate volume is not throttled and other accounts and routes are independent`() {
        val filter = ReadRateLimitFilter()
        authenticateAs("a")
        assertTrue((1..200).all { get(filter, "/api/feed") == 200 })
        assertTrue((1..200).all { get(filter, "/api/pets") == 200 })
        authenticateAs("b")
        assertEquals(200, get(filter, "/api/feed"))
    }

    @Test
    fun `costly routes are stricter than the general bucket and ids do not multiply buckets`() {
        val filter = ReadRateLimitFilter(settings = ReadLimitSettings(perMinute = 10, costlyPerMinute = 3))
        authenticateAs("c")
        val feed = (1..5).map { get(filter, "/api/feed/item-$it/comments") }
        assertEquals(listOf(200, 200, 200, 429, 429), feed)
        assertTrue((1..10).all { get(filter, "/api/pets/p$it") == 200 })
    }

    @Test
    fun `shadow mode never answers 429`() {
        val filter = ReadRateLimitFilter(settings = ReadLimitSettings(RateLimitSettings.Mode.SHADOW, 1, 1))
        authenticateAs("d")
        assertFalse((1..5).any { get(filter, "/api/feed") == 429 })
    }

    @Test
    fun `page above the maximum is rejected and wallet size is capped`() {
        org.junit.jupiter.api.assertThrows<com.pawmotionai.shared.PageOutOfRangeException> {
            com.pawmotionai.shared.PageRequest.of(com.pawmotionai.shared.PageRequest.maxPage + 1, 10)
        }
        assertEquals(50, com.pawmotionai.shared.PageRequest.of(0, 1_000_000).size)
    }
}
