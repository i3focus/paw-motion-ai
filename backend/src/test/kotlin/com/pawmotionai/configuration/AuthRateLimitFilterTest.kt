package com.pawmotionai.configuration

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AuthRateLimitFilterTest {

    private val filter = AuthRateLimitFilter("127.0.0.1,10.0.0.0/8,192.168.0.0/16")

    @Test
    fun ignoresForwardedHeadersFromUntrustedRemoteAddress() {
        val request = request("198.51.100.10", "203.0.113.8, 10.2.0.4", null)

        assertEquals("198.51.100.10", filter.clientKey(request))
    }

    @Test
    fun walksForwardedChainRightToLeftPastTrustedProxies() {
        val request = request("10.1.0.5", "203.0.113.8, 10.2.0.4", null)

        assertEquals("203.0.113.8", filter.clientKey(request))
    }

    @Test
    fun distinguishesVisitorsBehindTheSameIntermediateProxy() {
        val first = request("10.1.0.5", "203.0.113.8, 10.2.0.4", null)
        val second = request("10.1.0.5", "203.0.113.9, 10.2.0.4", null)

        assertEquals("203.0.113.8", filter.clientKey(first))
        assertEquals("203.0.113.9", filter.clientKey(second))
    }

    @Test
    fun returnsRemoteAddrWhenEveryForwardedHopIsTrusted() {
        val request = request("192.168.1.10", "10.1.0.2, 192.168.1.20", null)

        assertEquals("192.168.1.10", filter.clientKey(request))
    }

    @Test
    fun ignoresRealIpWithoutForwardedForToPreventSpoofing() {
        val request = request("10.1.0.5", null, "203.0.113.8")

        assertEquals("10.1.0.5", filter.clientKey(request))
    }

    private fun request(remoteAddr: String, forwardedFor: String?, realIp: String?): HttpServletRequest {
        val request = mock(HttpServletRequest::class.java)
        `when`(request.remoteAddr).thenReturn(remoteAddr)
        `when`(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor)
        `when`(request.getHeader("X-Real-IP")).thenReturn(realIp)
        return request
    }
}
