package com.pawmotionai.configuration

import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resolves the client key (IP) used by the rate-limit filters. Extracted from `AuthRateLimitFilter` without behavior
 * change: forwarded headers are honored only when the connection comes from a trusted proxy, and the chain is walked
 * right to left past trusted proxies.
 */
class ClientIpResolver(trustedProxies: String?) {
    private val trustedProxies: List<IpCidr> = parseTrustedProxies(trustedProxies)

    fun clientKey(request: HttpServletRequest): String {
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

    private companion object {
        fun parseTrustedProxies(value: String?): List<IpCidr> {
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
