package com.pawmotionai.aiinsights.application.port.out;

import java.util.UUID;

public record InsightPrompt(
    UUID ownerId,
    UUID petId,
    String petName,
    int periodDays,
    int activityCount,
    long durationSeconds,
    double distanceMeters) {
}
