package com.pawmotionai.configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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

    private final List<IpCidr> trustedProxies;
    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(String trustedProxies) {
        this.trustedProxies = parseTrustedProxies(trustedProxies);
    }

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

    String clientKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            List<String> hops = Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .filter(hop -> !hop.isEmpty())
                .toList();
            for (int index = hops.size() - 1; index >= 0; index--) {
                String hop = hops.get(index);
                if (!isTrustedProxy(hop)) {
                    return hop;
                }
            }
            if (!hops.isEmpty()) {
                return hops.get(0);
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return trustedProxies.stream().anyMatch(proxy -> proxy.matches(remoteAddr));
    }

    private static List<IpCidr> parseTrustedProxies(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(proxy -> !proxy.isEmpty())
            .map(IpCidr::parse)
            .toList();
    }

    private record IpCidr(byte[] address, int prefixLength) {

        static IpCidr parse(String value) {
            String[] parts = value.split("/", 2);
            String host = parts[0].trim();
            byte[] address;
            try {
                address = InetAddress.getByName(host).getAddress();
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException("Invalid trusted proxy address: " + host, exception);
            }
            int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1].trim()) : address.length * 8;
            if (prefixLength < 0 || prefixLength > address.length * 8) {
                throw new IllegalArgumentException("Invalid trusted proxy prefix: " + value);
            }
            return new IpCidr(address, prefixLength);
        }

        boolean matches(String candidateValue) {
            if (candidateValue == null || candidateValue.isBlank()) {
                return false;
            }
            final byte[] candidate;
            try {
                candidate = InetAddress.getByName(candidateValue.trim()).getAddress();
            } catch (UnknownHostException exception) {
                return false;
            }
            if (candidate.length != address.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (candidate[index] != address[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits) & 0xFF;
            return (candidate[fullBytes] & mask) == (address[fullBytes] & mask);
        }
    }

    private record Window(long startedAtEpochMilli, int count) {
    }
}
