package com.pawmotionai.activity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RecordActivityCommand(
    UUID ownerId,
    UUID petId,
    String type,
    Instant startedAt,
    long durationSeconds,
    Double distanceMeters,
    String notes) {
}
