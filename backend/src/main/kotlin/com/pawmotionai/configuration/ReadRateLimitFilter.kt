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
 * Read bucket per `(subject, normalized route)` for GET requests (C16). Route = first two path segments (`/api/feed`),
 * so ids never multiply buckets. Costly routes get the stricter [ReadLimitSettings.costlyPerMinute]; the rest use
 * [ReadLimitSettings.perMinute]. Defaults are generous. Enforced by default; `shadow` only records
 * `ratelimit.shadow_violations` (rollback). In-memory per instance, like the other filters.
 */
class ReadRateLimitFilter(
    private val clientIpResolver: ClientIpResolver = ClientIpResolver(""),
    private val settings: ReadLimitSettings = ReadLimitSettings(),
    private val meterRegistry: MeterRegistry? = null,
) : OncePerRequestFilter() {
    private val windows = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "GET" || !RequestPaths.hasSegmentPrefix(RequestPaths.normalized(request), "/api")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val now = Instant.now().toEpochMilli()
        val route = route(RequestPaths.normalized(request))
        val costly = route in COSTLY_ROUTES
        val max = if (costly) settings.costlyPerMinute else settings.perMinute
        val subject = subject(clientIpResolver.clientKey(request))
        if (exceeds("r:$subject:$route", now, max)) {
            val bucket = if (costly) "read-costly" else "read"
            if (settings.mode == RateLimitSettings.Mode.SHADOW) {
                meterRegistry?.counter("ratelimit.shadow_violations", "bucket", bucket, "route", route)?.increment()
            } else {
                meterRegistry?.counter("ratelimit.rejected", "bucket", bucket, "route", route)?.increment()
                response.sendError(429, "Too many requests")
                return
            }
        }
        if (windows.size > MAX_TRACKED_KEYS) {
            windows.entries.removeIf { now - it.value.startedAtEpochMilli >= WINDOW.toMillis() }
        }
        filterChain.doFilter(request, response)
    }

    private fun route(path: String): String = path.split("/").filter { it.isNotEmpty() }.take(2).joinToString("/", "/")

    private fun exceeds(key: String, now: Long, max: Int): Boolean {
        val window = windows.compute(key) { _, existing ->
            if (existing == null || now - existing.startedAtEpochMilli >= WINDOW.toMillis()) {
                Window(now, 1)
            } else {
                Window(existing.startedAtEpochMilli, existing.count + 1)
            }
        }
        return window != null && window.count > max
    }

    private fun subject(ip: String): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val name = authentication?.name
        return if (authentication == null || !authentication.isAuthenticated || name.isNullOrBlank()
            || name == "anonymousUser") "ip:$ip" else name
    }

    private data class Window(val startedAtEpochMilli: Long, val count: Int)

    private companion object {
        val COSTLY_ROUTES = setOf("/api/feed", "/api/places", "/api/leaderboard", "/api/discover")
        val WINDOW: Duration = Duration.ofMinutes(1)
        const val MAX_TRACKED_KEYS = 20_000
    }
}

/** `app.rate-limit.read.*`: generous defaults so legitimate use (scrolling, polling) never reaches 429. */
data class ReadLimitSettings(
    val mode: RateLimitSettings.Mode = RateLimitSettings.Mode.ENFORCE,
    val perMinute: Int = 600,
    val costlyPerMinute: Int = 240,
)
