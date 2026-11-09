package com.pawmotionai.activity.application.dto;

import java.util.List;
import java.util.UUID;

public record DashboardSummaryDto(
    UUID ownerId,
    int petCount,
    int activityCountLast7Days,
    double totalDistanceMetersLast7Days,
    long totalDurationSecondsLast7Days,
    int currentStreakDays,
    List<ActivityResponseDto> recentActivities) {
}
