package com.pawmotionai.activity.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import com.pawmotionai.activity.domain.model.Activity;

public final class ActivityStatisticsCalculator {

    private ActivityStatisticsCalculator() {
    }

    public static int currentStreakDays(java.util.Collection<Activity> activities, Instant now) {
        Set<LocalDate> activeDays = activities.stream()
            .map(activity -> activity.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate())
            .collect(Collectors.toSet());
        LocalDate cursor = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (!activeDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
