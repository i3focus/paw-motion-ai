package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 11.3 (N4): actuator is public only for `health` without details; info, metrics and the rest need ADMIN.
 * Shares the default Spring context (no extra pool against the shared Postgres). Validates: Requirements 2.26, 3.14.
 */
class EdgeActuatorIT : PreservationApiTest() {
    @Test
    fun `health is public without details while info and metrics are admin only`() {
        val health = call("GET", "/actuator/health", null)
        assertEquals(200, health.status)
        assertFalse(health.body.contains("components") || health.body.contains("details"), health.body)
        val owner = registerOwner("Comum")
        val admin = registerOwner("Boss", role = "ADMIN")
        for (path in listOf("/actuator/info", "/actuator/metrics", "/actuator/metrics/jvm.memory.used", "/actuator/env")) {
            assertEquals(401, call("GET", path, null).status, "anonymous $path")
            assertEquals(403, call("GET", path, owner.token).status, "owner $path")
        }
        assertEquals(200, call("GET", "/actuator/info", admin.token).status)
        assertEquals(200, call("GET", "/actuator/metrics", admin.token).status)
    }
}
