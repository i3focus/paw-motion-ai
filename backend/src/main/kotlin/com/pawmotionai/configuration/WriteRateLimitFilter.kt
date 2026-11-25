package com.pawmotionai.configuration

import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limit on mutating API routes (plan §6). Routes are classified by the normalized path (C25, C26).
 * - Authenticated: `w:<account>` (120/min) plus `ai:<account>` (30/min) for assistant messages and insights (C17).
 * - Anonymous: `w:ip:<client IP>` (120/min), so one IP never throttles another (C14).
 * - `/api/webhooks/payments` and below: own bucket `wh:<ip>`, independent of login and write buckets. Governed by
 *   [RateLimitSettings.mode] (starts in shadow).
 * Buckets are in-memory per instance, like [AuthRateLimitFilter].
 */
class WriteRateLimitFilter(
    private val clientIpResolver: ClientIpResolver = ClientIpResolver(""),
    private val settings: RateLimitSettings = RateLimitSettings(),
    private val meterRegistry: MeterRegistry? = null,
) : OncePerRequestFilter() {
    private val attempts = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!RequestPaths.hasSegmentPrefix(RequestPaths.normalized(request), "/api")) {
            return true
        }
        val method = request.method
        return method == "GET" || method == "HEAD" || method == "OPTIONS"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val now = Instant.now().toEpochMilli()
        val path = RequestPaths.normalized(request)
        val ip = clientIpResolver.clientKey(request)
        if (RequestPaths.hasSegmentPrefix(path, WEBHOOK_PREFIX)) {
            if (exceeds("wh:$ip", now, settings.webhookPerMinute) && reject("webhook", "webhook", response)) {
                return
            }
        } else {
            val subject = subject(ip)
            if (exceeds("w:$subject", now, MAX_WRITES_PER_WINDOW) && reject("write", "write", response)) {
                return
            }
            if (isAiMessage(request, path) && exceeds("ai:$subject", now, MAX_AI_MESSAGES_PER_WINDOW)
                && reject("ai", if (RequestPaths.hasSegmentPrefix(path, "/api/insights")) "insights" else "assistant", response)) {
                return
            }
        }
        if (attempts.size > MAX_TRACKED_KEYS) {
            attempts.entries.removeIf { now - it.value.startedAtEpochMilli >= WINDOW.toMillis() }
        }
        filterChain.doFilter(request, response)
    }

    /** Records the violation. Returns true (and answers 429) for enforced buckets, false for shadow-mode ones. */
    private fun reject(bucket: String, route: String, response: HttpServletResponse): Boolean {
        val shadow = bucket == "webhook" && settings.mode == RateLimitSettings.Mode.SHADOW
        if (shadow) {
            meterRegistry?.counter("ratelimit.shadow_violations", "bucket", bucket, "route", route)?.increment()
            return false
        }
        meterRegistry?.counter("ratelimit.rejected", "bucket", bucket, "route", route)?.increment()
        response.sendError(429, "Too many requests")
        return true
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

    private fun isAiMessage(request: HttpServletRequest, path: String): Boolean =
        request.method == "POST" && (RequestPaths.hasSegmentPrefix(path, "/api/assistant") || RequestPaths.hasSegmentPrefix(path, "/api/insights"))

    private fun subject(ip: String): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val name = authentication?.name
        return if (authentication == null || !authentication.isAuthenticated || name.isNullOrBlank()
            || name == "anonymousUser") "ip:$ip" else name
    }

    private data class Window(val startedAtEpochMilli: Long, val count: Int)

    private companion object {
        const val MAX_WRITES_PER_WINDOW = 120
        const val MAX_AI_MESSAGES_PER_WINDOW = 30
        const val WEBHOOK_PREFIX = "/api/webhooks/payments"
        val WINDOW: Duration = Duration.ofMinutes(1)
        const val MAX_TRACKED_KEYS = 20_000
    }
}
