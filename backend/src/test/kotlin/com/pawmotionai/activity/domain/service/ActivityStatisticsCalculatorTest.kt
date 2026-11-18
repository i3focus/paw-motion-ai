package com.pawmotionai.activity.domain.service

import com.pawmotionai.activity.domain.model.Activity
import com.pawmotionai.activity.domain.model.ActivityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ActivityStatisticsCalculatorTest {

    @Test
    fun currentStreakCountsConsecutiveDays() {
        val now = Instant.parse("2026-11-09T12:00:00Z")
        val ownerId = UUID.randomUUID()
        val petId = UUID.randomUUID()
        val activities = listOf(
            activity(ownerId, petId, now.minus(1, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(2, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(3, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(5, ChronoUnit.DAYS)))

        assertEquals(3, ActivityStatisticsCalculator.currentStreakDays(activities, now))
    }

    @Test
    fun currentStreakAllowsTodayToBeMissing() {
        val now = Instant.parse("2026-11-09T12:00:00Z")
        val ownerId = UUID.randomUUID()
        val petId = UUID.randomUUID()
        val activities = listOf(
            activity(ownerId, petId, now.minus(1, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(2, ChronoUnit.DAYS)))

        assertEquals(2, ActivityStatisticsCalculator.currentStreakDays(activities, now))
    }

    private fun activity(ownerId: UUID, petId: UUID, startedAt: Instant): Activity =
        Activity.reconstitute(
            UUID.randomUUID(), ownerId, petId, ActivityType.WALK, startedAt, 1800, 1000.0, null, startedAt)
}
