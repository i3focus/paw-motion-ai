package com.pawmotionai.configuration

import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress

/**
 * Resolves the client key (IP) used by the rate-limit filters. Forwarded headers are honored only when the connection
 * comes from a trusted proxy. The chain is walked right to left past trusted proxies; the first untrusted hop is the
 * client. When every hop is trusted (or a hop is not an IP literal) the key is the connection peer, never a value the
 * sender chose (N13). Addresses are parsed as literals only, so header text never triggers DNS.
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
                val address = LiteralIp.parse(hop) ?: return remoteAddr // not an IP literal: never trust or key on it
                if (!isTrustedProxy(hop)) {
                    return LiteralIp.canonical(address)
                }
            }
        }
        // No header, or every hop is a trusted proxy: the last trusted hop is the connection peer. Never hops[0],
        // which is whatever the sender wrote (N13).
        return remoteAddr
    }

    private fun isTrustedProxy(remoteAddr: String): Boolean =
        trustedProxies.any { it.matches(remoteAddr) }

    private class IpCidr(private val address: ByteArray, private val prefixLength: Int) {
        fun matches(candidateValue: String?): Boolean {
            if (candidateValue.isNullOrBlank()) {
                return false
            }
            val candidate = LiteralIp.parse(candidateValue.trim())?.address ?: return false
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
                val address = (LiteralIp.parse(host) ?: throw IllegalArgumentException("Invalid trusted proxy address: $host")).address
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

/** Strict IP literal parser: never calls the resolver, so header text cannot cause DNS lookups. */
internal object LiteralIp {
    private val IPV4 = Regex("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$")
    private val IPV6_CHARS = Regex("^[0-9a-fA-F:.]+$")

    fun parse(value: String?): InetAddress? {
        val text = value?.trim()
        if (text.isNullOrEmpty() || text.length > 45) return null
        val literal = when {
            IPV4.matches(text) -> text
            ':' in text && IPV6_CHARS.matches(text) -> text
            else -> return null
        }
        // A literal (digits/colons/dots only, at least one ':' or a strict dotted quad) is parsed without lookups.
        return try {
            InetAddress.getByName(literal)
        } catch (exception: Exception) {
            null
        }
    }

    fun canonical(address: InetAddress): String = address.hostAddress
}
