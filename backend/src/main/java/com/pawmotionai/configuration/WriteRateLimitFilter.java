package com.pawmotionai.configuration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-user rate limit on mutating API routes (plan §6). AI assistant messages get a stricter bucket on top of the
 * general write bucket. Buckets are in-memory per instance, like {@link AuthRateLimitFilter}.
 */
public class WriteRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_WRITES_PER_WINDOW = 120;
    private static final int MAX_AI_MESSAGES_PER_WINDOW = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }
        String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long now = Instant.now().toEpochMilli();
        String subject = subject();
        if (exceeds("w:" + subject, now, MAX_WRITES_PER_WINDOW)
            || (isAiMessage(request) && exceeds("ai:" + subject, now, MAX_AI_MESSAGES_PER_WINDOW))) {
            response.sendError(429, "Too many requests");
            return;
        }
        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(entry -> now - entry.getValue().startedAtEpochMilli() >= WINDOW.toMillis());
        }
        filterChain.doFilter(request, response);
    }

    private boolean exceeds(String key, long now, int max) {
        Window window = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startedAtEpochMilli() >= WINDOW.toMillis()) {
                return new Window(now, 1);
            }
            return new Window(existing.startedAtEpochMilli(), existing.count() + 1);
        });
        return window.count() > max;
    }

    private boolean isAiMessage(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && request.getRequestURI().contains("/api/assistant/");
    }

    private String subject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    private record Window(long startedAtEpochMilli, int count) {
    }
}
