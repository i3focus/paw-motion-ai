package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/** PR 5a (C3, C21): former exploratory tests, now regular. Validates: Requirements 2.21, 3.10, 3.13. */
class ActivityBoundsIT : ExploratoryApiTest() {

    private fun activityJson(petId: UUID, startedAt: String, duration: String, distance: String) =
        """{"petId":"$petId","type":"walk","startedAt":"$startedAt","durationSeconds":$duration,"distanceMeters":$distance}"""

    private fun activityCount(petId: UUID) =
        jdbc.queryForObject("SELECT COUNT(*) FROM activities WHERE pet_id = ?", Long::class.java, petId)

    private fun post(duration: String = "600", distance: String = "1000", startedAt: String = Instant.now().minusSeconds(60).toString()): Pair<Int, Long?> {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        val response = rawJsonCall("POST", "/api/activities", owner.token, activityJson(pet, startedAt, duration, distance))
        return response.status to activityCount(pet)
    }

    @ParameterizedTest(name = "C3 durationSeconds={0}")
    @ValueSource(strings = ["9223372036854775807", "86401", "31536000"])
    fun `C3 absurd duration is rejected with 400`(duration: String) {
        val (status, rows) = post(duration = duration)
        assertEquals(400, status, "Counterexample C3: durationSeconds=$duration -> HTTP $status, rows=$rows")
        assertEquals(0L, rows)
    }

    @ParameterizedTest(name = "C3 distanceMeters={0}")
    @ValueSource(strings = ["500001", "1e15", "NaN", "Infinity", "-Infinity"])
    fun `C3 absurd or non finite distance is rejected with 400`(distance: String) {
        val (status, rows) = post(distance = distance)
        assertEquals(400, status, "Counterexample C3: distanceMeters=$distance -> HTTP $status, rows=$rows")
        assertEquals(0L, rows)
    }

    @ParameterizedTest(name = "C21 startedAt={0}")
    @ValueSource(strings = ["1970-01-01T00:00:00Z", "2000-06-15T12:00:00Z"])
    fun `C21 activity far in the past is rejected with 400`(startedAt: String) {
        val (status, rows) = post(startedAt = startedAt)
        assertEquals(400, status, "Counterexample C21: startedAt=$startedAt -> HTTP $status, rows=$rows")
        assertEquals(0L, rows)
    }

    @Test
    fun `C21 startedAt 15 days back is rejected`() {
        assertEquals(400, post(startedAt = Instant.now().minus(Duration.ofDays(15)).toString()).first)
    }

    @Test
    fun `values at the exact limits are accepted`() {
        val (status, rows) = post(duration = "86400", distance = "500000", startedAt = Instant.now().minus(Duration.ofDays(13)).toString())
        assertEquals(201, status)
        assertEquals(1L, rows)
    }

    @Test
    fun `database constraints reject out of range rows but legacy rows stay valid for NOT VALID`() {
        val owner = registerOwner("Bia")
        val pet = createPet(owner)
        val violation = runCatching {
            jdbc.update(
                "INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at) " +
                    "VALUES (gen_random_uuid(), ?, ?, 'WALK', now(), 999999, 1, now(), now())", owner.ownerId, pet)
        }
        assertEquals(true, violation.isFailure, "chk_activities_duration_range must reject new extreme rows")
        assertEquals(0L, activityCount(pet))
    }
}
