package com.pawmotionai.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-user rate limit on mutating API routes (plan §6). AI assistant messages get a stricter bucket on top of the
 * general write bucket. Buckets are in-memory per instance, like [AuthRateLimitFilter].
 */
class WriteRateLimitFilter : OncePerRequestFilter() {

    private val attempts = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!request.requestURI.startsWith("/api/")) {
            return true
        }
        val method = request.method
        return method == "GET" || method == "HEAD" || method == "OPTIONS"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val now = Instant.now().toEpochMilli()
        val subject = subject()
        if (exceeds("w:$subject", now, MAX_WRITES_PER_WINDOW)
            || (isAiMessage(request) && exceeds("ai:$subject", now, MAX_AI_MESSAGES_PER_WINDOW))) {
            response.sendError(429, "Too many requests")
            return
        }
        if (attempts.size > MAX_TRACKED_KEYS) {
            attempts.entries.removeIf { now - it.value.startedAtEpochMilli >= WINDOW.toMillis() }
        }
        filterChain.doFilter(request, response)
    }

    private fun exceeds(key: String, now: Long, max: Int): Boolean {
        val window = attempts.compute(key) { _, existing ->
            if (existing == null || now - existing.startedAtEpochMilli >= WINDOW.toMillis()) {
                Window(now, 1)
            } else {
                Window(existing.startedAtEpochMilli, existing.count + 1)
            }
        }
        return window != null && window.count > max
    }

    private fun isAiMessage(request: HttpServletRequest): Boolean =
        request.method == "POST" && request.requestURI.contains("/api/assistant/")

    private fun subject(): String =
        SecurityContextHolder.getContext().authentication?.name ?: "anonymous"

    private data class Window(val startedAtEpochMilli: Long, val count: Int)

    private companion object {
        const val MAX_WRITES_PER_WINDOW = 120
        const val MAX_AI_MESSAGES_PER_WINDOW = 30
        val WINDOW: Duration = Duration.ofMinutes(1)
        const val MAX_TRACKED_KEYS = 20_000
    }
}
