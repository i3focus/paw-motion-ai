package com.pawmotionai.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class AuthRateLimitFilter(trustedProxies: String) : OncePerRequestFilter() {

    private val trustedProxies: List<IpCidr> = parseTrustedProxies(trustedProxies)
    private val attempts = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !(path == "/api/auth/login" || path == "/api/auth/register"
            || path == "/api/partner/auth/login" || path == "/api/partner/auth/register")
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val now = Instant.now().toEpochMilli()
        val key = "${clientKey(request)}:${request.requestURI}"
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
            response.sendError(429, "Too many authentication attempts")
            return
        }
        filterChain.doFilter(request, response)
    }

    internal fun clientKey(request: HttpServletRequest): String {
        val remoteAddr = request.remoteAddr
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr
        }
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            val hops = forwardedFor.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (index in hops.indices.reversed()) {
                val hop = hops[index]
                if (!isTrustedProxy(hop)) {
                    return hop
                }
            }
            if (hops.isNotEmpty()) {
                return hops[0]
            }
        }
        return remoteAddr
    }

    private fun isTrustedProxy(remoteAddr: String): Boolean =
        trustedProxies.any { it.matches(remoteAddr) }

    private data class Window(val startedAtEpochMilli: Long, val count: Int)

    private class IpCidr(private val address: ByteArray, private val prefixLength: Int) {

        fun matches(candidateValue: String?): Boolean {
            if (candidateValue.isNullOrBlank()) {
                return false
            }
            val candidate = try {
                InetAddress.getByName(candidateValue.trim()).address
            } catch (exception: UnknownHostException) {
                return false
            }
            if (candidate.size != address.size) {
                return false
            }
            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8
            for (index in 0 until fullBytes) {
                if (candidate[index] != address[index]) {
                    return false
                }
            }
            if (remainingBits == 0) {
                return true
            }
            val mask = 0xFF shl (8 - remainingBits) and 0xFF
            return candidate[fullBytes].toInt() and mask == address[fullBytes].toInt() and mask
        }

        companion object {
            fun parse(value: String): IpCidr {
                val parts = value.split("/", limit = 2)
                val host = parts[0].trim()
                val address = try {
                    InetAddress.getByName(host).address
                } catch (exception: UnknownHostException) {
                    throw IllegalArgumentException("Invalid trusted proxy address: $host", exception)
                }
                val prefixLength = if (parts.size == 2) parts[1].trim().toInt() else address.size * 8
                require(prefixLength in 0..address.size * 8) { "Invalid trusted proxy prefix: $value" }
                return IpCidr(address, prefixLength)
            }
        }
    }

    companion object {
        private const val MAX_REQUESTS_PER_WINDOW = 10
        private val WINDOW: Duration = Duration.ofMinutes(1)
        private const val MAX_TRACKED_KEYS = 10_000

        private fun parseTrustedProxies(value: String?): List<IpCidr> {
            if (value.isNullOrBlank()) {
                return emptyList()
            }
            return value.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map(IpCidr::parse)
        }
    }
}
