package com.pawmotionai.preservation

import com.pawmotionai.configuration.AuthRateLimitFilter
import com.pawmotionai.configuration.WriteRateLimitFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.random.Random

/**
 * Preservation 3.7: the existing limits (10 auth attempts/min/IP/route, 120 writes/min/account, 30 AI messages/min)
 * return 429 on the same boundary, and normal traffic never gets 429. Canonical paths only, so no `isBugCondition_C25/C26`.
 * Properties run over seeded random inputs, the seed is in the test name so a failure can be replayed.
 */
class RateLimitPreservationTest {

    private val trusted = "127.0.0.1,10.0.0.0/8"
    private val authRoutes = listOf("/api/auth/login", "/api/auth/register", "/api/partner/auth/login", "/api/partner/auth/register")

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun send(filter: OncePerRequestFilter, method: String, uri: String, remote: String): Int {
        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest(method, uri).apply { remoteAddr = remote }, response, MockFilterChain())
        return response.status
    }

    private fun authenticateAs(name: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(name, null, emptyList())
    }

    private fun publicIp(random: Random) = "${random.nextInt(11, 100)}.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}.${random.nextInt(1, 255)}"

    @ParameterizedTest(name = "auth boundary, seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5, 6, 7, 8])
    fun `auth routes answer 200 for 10 attempts and 429 from the 11th, per IP and per route`(seed: Long) {
        val random = Random(seed)
        val filter = AuthRateLimitFilter(trusted)
        val route = authRoutes[random.nextInt(authRoutes.size)]
        val ip = publicIp(random)
        val otherIp = publicIp(random).let { if (it == ip) "198.51.100.200" else it }
        val otherRoute = authRoutes.first { it != route }

        val statuses = (1..12).map { send(filter, "POST", route, ip) }
        assertEquals(List(10) { 200 } + listOf(429, 429), statuses, "seed=$seed route=$route ip=$ip")
        // Another IP and another route keep their own windows.
        assertEquals(200, send(filter, "POST", route, otherIp), "seed=$seed: other IP was throttled")
        assertEquals(200, send(filter, "POST", otherRoute, ip), "seed=$seed: other route was throttled")
    }

    @ParameterizedTest(name = "auth normal traffic, seed {0}")
    @ValueSource(longs = [11, 12, 13, 14])
    fun `normal auth traffic below the limit never gets 429`(seed: Long) {
        val random = Random(seed)
        val filter = AuthRateLimitFilter(trusted)
        val attemptsPerKey = mutableMapOf<Pair<String, String>, Int>()
        repeat(200) {
            val ip = "203.0.113.${random.nextInt(1, 40)}"
            val route = authRoutes[random.nextInt(authRoutes.size)]
            val key = ip to route
            if ((attemptsPerKey[key] ?: 0) < 10) {
                attemptsPerKey[key] = (attemptsPerKey[key] ?: 0) + 1
                assertEquals(200, send(filter, "POST", route, ip), "seed=$seed $key")
            }
        }
    }

    @Test
    fun `auth filter ignores routes outside the four auth endpoints and non-api paths`() {
        val filter = AuthRateLimitFilter(trusted)
        repeat(30) { assertEquals(200, send(filter, "POST", "/api/pets", "198.51.100.1")) }
        repeat(30) { assertEquals(200, send(filter, "GET", "/api/places", "198.51.100.1")) }
    }

    @ParameterizedTest(name = "write boundary, seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5, 6])
    fun `authenticated writes answer 200 for 120 requests per minute and 429 afterwards, isolated per account`(seed: Long) {
        val random = Random(seed)
        val filter = WriteRateLimitFilter()
        val method = listOf("POST", "PUT", "PATCH", "DELETE")[random.nextInt(4)]
        val path = listOf("/api/pets", "/api/activities", "/api/feed/x/comments", "/api/lost-pets")[random.nextInt(4)]
        authenticateAs("account-a-$seed")
        val statuses = (1..122).map { send(filter, method, path, "198.51.100.1") }
        assertEquals(List(120) { 200 } + listOf(429, 429), statuses, "seed=$seed $method $path")
        authenticateAs("account-b-$seed")
        assertEquals(200, send(filter, method, path, "198.51.100.1"), "seed=$seed: another account was throttled")
    }

    @Test
    fun `AI assistant messages are limited to 30 per minute on top of the general bucket`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("ai-user")
        val path = "/api/assistant/conversations/1/messages"
        val statuses = (1..32).map { send(filter, "POST", path, "198.51.100.1") }
        assertEquals(List(30) { 200 } + listOf(429, 429), statuses)
        // The general write bucket of the same account is unaffected up to its own limit (30 used so far).
        assertEquals(200, send(filter, "POST", "/api/pets", "198.51.100.1"))
    }

    @Test
    fun `normal write traffic by many accounts and a few anonymous calls never gets 429`() {
        val filter = WriteRateLimitFilter()
        repeat(40) { account ->
            authenticateAs("normal-$account")
            repeat(30) { assertEquals(200, send(filter, "POST", "/api/activities", "198.51.100.1")) }
        }
        SecurityContextHolder.clearContext()
        repeat(20) { assertEquals(200, send(filter, "POST", "/api/auth/login", "198.51.100.2")) }
    }

    @Test
    fun `non-api paths are not subject to the write limit`() {
        val filter = WriteRateLimitFilter()
        authenticateAs("static-user")
        repeat(150) { assertEquals(200, send(filter, "POST", "/actuator/health", "198.51.100.1")) }
    }
}
