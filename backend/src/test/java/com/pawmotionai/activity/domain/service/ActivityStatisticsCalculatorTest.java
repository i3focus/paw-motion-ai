package com.pawmotionai.activity.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.pawmotionai.activity.domain.model.Activity;
import com.pawmotionai.activity.domain.model.ActivityType;

class ActivityStatisticsCalculatorTest {

    @Test
    void currentStreakCountsConsecutiveDays() {
        Instant now = Instant.parse("2026-11-09T12:00:00Z");
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        List<Activity> activities = List.of(
            activity(ownerId, petId, now.minus(1, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(2, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(3, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(5, ChronoUnit.DAYS)));

        assertEquals(3, ActivityStatisticsCalculator.currentStreakDays(activities, now));
    }

    @Test
    void currentStreakAllowsTodayToBeMissing() {
        Instant now = Instant.parse("2026-11-09T12:00:00Z");
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        List<Activity> activities = List.of(
            activity(ownerId, petId, now.minus(1, ChronoUnit.DAYS)),
            activity(ownerId, petId, now.minus(2, ChronoUnit.DAYS)));

        assertEquals(2, ActivityStatisticsCalculator.currentStreakDays(activities, now));
    }

    private Activity activity(UUID ownerId, UUID petId, Instant startedAt) {
        return Activity.reconstitute(
            UUID.randomUUID(), ownerId, petId, ActivityType.WALK, startedAt, 1800, 1000.0, null, startedAt);
    }
}
