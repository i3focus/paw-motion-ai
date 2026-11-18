package com.pawmotionai.activity.domain.service

import com.pawmotionai.activity.domain.model.Activity
import java.time.Instant
import java.time.ZoneOffset

object ActivityStatisticsCalculator {

    fun currentStreakDays(activities: Collection<Activity>, now: Instant): Int {
        val activeDays = activities
            .map { it.startedAt.atZone(ZoneOffset.UTC).toLocalDate() }
            .toSet()
        var cursor = now.atZone(ZoneOffset.UTC).toLocalDate()
        if (!activeDays.contains(cursor)) {
            cursor = cursor.minusDays(1)
        }
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
