package com.pawmotionai.configuration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("/api/auth/login".equals(path) || "/api/auth/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long now = Instant.now().toEpochMilli();
        String key = clientKey(request) + ":" + request.getRequestURI();
        Window window = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startedAtEpochMilli() >= WINDOW.toMillis()) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAtEpochMilli(), existing.count() + 1);
        });
        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(entry -> now - entry.getValue().startedAtEpochMilli() >= WINDOW.toMillis());
        }
        if (window.count() > MAX_REQUESTS_PER_WINDOW) {
            response.sendError(429, "Too many authentication attempts");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            return hops[hops.length - 1].trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        if ("127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return true;
        }
        if (remoteAddr.startsWith("10.") || remoteAddr.startsWith("192.168.") || remoteAddr.startsWith("169.254.")) {
            return true;
        }
        if (remoteAddr.startsWith("172.")) {
            String[] parts = remoteAddr.split("\\.");
            if (parts.length == 4) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private record Window(long startedAtEpochMilli, int count) {
    }
}
