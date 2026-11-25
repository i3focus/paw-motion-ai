package com.pawmotionai.activity.domain.model

import com.pawmotionai.activity.domain.exception.ActivityOutOfRangeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** PR 5a (C3, C21). Validates: Requirements 2.21, 3.10. */
class ActivityBoundsTest {
    private val now = Instant.parse("2026-01-20T12:00:00Z")

    private fun record(duration: Long = 600, distance: Double? = 1000.0, startedAt: Instant = now.minusSeconds(60)) =
        Activity.record(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ActivityType.WALK, startedAt,
            duration, distance, null, now, now = now)

    @Test
    fun `accepts exact limits`() {
        record(duration = 86_400, distance = 500_000.0, startedAt = now.minus(Duration.ofDays(14)))
        record(startedAt = now.plus(Duration.ofMinutes(5)))
    }

    @Test
    fun `rejects out of range values`() {
        assertThrows<ActivityOutOfRangeException> { record(duration = 86_401) }
        assertThrows<ActivityOutOfRangeException> { record(distance = 500_000.1) }
        assertThrows<ActivityOutOfRangeException> { record(distance = Double.NaN) }
        assertThrows<ActivityOutOfRangeException> { record(distance = Double.POSITIVE_INFINITY) }
        assertThrows<ActivityOutOfRangeException> { record(startedAt = now.minus(Duration.ofDays(14)).minusSeconds(1)) }
    }
}
