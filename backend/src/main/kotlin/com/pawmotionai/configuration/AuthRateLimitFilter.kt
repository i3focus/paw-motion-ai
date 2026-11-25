package com.pawmotionai.configuration

import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** 10 attempts/min per IP and route on the auth endpoints. Routes are classified by the normalized path (C25). */
class AuthRateLimitFilter(
    private val clientIpResolver: ClientIpResolver,
    private val meterRegistry: MeterRegistry? = null,
) : OncePerRequestFilter() {
    constructor(trustedProxies: String) : this(ClientIpResolver(trustedProxies))

    private val attempts = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = RequestPaths.normalized(request) !in AUTH_ROUTES

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val now = Instant.now().toEpochMilli()
        val path = RequestPaths.normalized(request)
        val key = "${clientKey(request)}:$path"
        val window = attempts.compute(key) { _, existing ->
            if (existing == null || now - existing.startedAtEpochMilli >= WINDOW.toMillis()) {
                Window(now, 1)
            } else {
                Window(existing.startedAtEpochMilli, existing.count + 1)
            }
        }
        if (attempts.size > MAX_TRACKED_KEYS) {
            attempts.entries.removeIf { now - it.value.startedAtEpochMilli >= WINDOW.toMillis() }
        }
        if (window != null && window.count > MAX_REQUESTS_PER_WINDOW) {
            meterRegistry?.counter("ratelimit.rejected", "bucket", "auth", "route", path)?.increment()
            response.sendError(429, "Too many authentication attempts")
            return
        }
        filterChain.doFilter(request, response)
    }

    internal fun clientKey(request: HttpServletRequest): String = clientIpResolver.clientKey(request)

    private data class Window(val startedAtEpochMilli: Long, val count: Int)

    companion object {
        private const val MAX_REQUESTS_PER_WINDOW = 10
        private val WINDOW: Duration = Duration.ofMinutes(1)
        private const val MAX_TRACKED_KEYS = 10_000
        private val AUTH_ROUTES = setOf(
            "/api/auth/login", "/api/auth/register", "/api/partner/auth/login", "/api/partner/auth/register",
        )
    }
}
