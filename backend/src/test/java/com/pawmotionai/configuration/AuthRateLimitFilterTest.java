package com.pawmotionai.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(
        "127.0.0.1,10.0.0.0/8,192.168.0.0/16");

    @Test
    void ignoresForwardedHeadersFromUntrustedRemoteAddress() {
        HttpServletRequest request = request("198.51.100.10", "203.0.113.8, 10.2.0.4", null);

        assertEquals("198.51.100.10", filter.clientKey(request));
    }

    @Test
    void walksForwardedChainRightToLeftPastTrustedProxies() {
        HttpServletRequest request = request("10.1.0.5", "203.0.113.8, 10.2.0.4", null);

        assertEquals("203.0.113.8", filter.clientKey(request));
    }

    @Test
    void distinguishesVisitorsBehindTheSameIntermediateProxy() {
        HttpServletRequest first = request("10.1.0.5", "203.0.113.8, 10.2.0.4", null);
        HttpServletRequest second = request("10.1.0.5", "203.0.113.9, 10.2.0.4", null);

        assertEquals("203.0.113.8", filter.clientKey(first));
        assertEquals("203.0.113.9", filter.clientKey(second));
    }

    @Test
    void returnsLeftmostHopWhenEveryForwardedHopIsTrusted() {
        HttpServletRequest request = request("192.168.1.10", "10.1.0.2, 192.168.1.20", null);

        assertEquals("10.1.0.2", filter.clientKey(request));
    }

    @Test
    void ignoresRealIpWithoutForwardedForToPreventSpoofing() {
        HttpServletRequest request = request("10.1.0.5", null, "203.0.113.8");

        assertEquals("10.1.0.5", filter.clientKey(request));
    }

    private HttpServletRequest request(String remoteAddr, String forwardedFor, String realIp) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        when(request.getHeader("X-Real-IP")).thenReturn(realIp);
        return request;
    }
}
