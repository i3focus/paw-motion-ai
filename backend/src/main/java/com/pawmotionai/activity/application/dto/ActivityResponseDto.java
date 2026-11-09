package com.pawmotionai.activity.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.activity.domain.model.Activity;

public record ActivityResponseDto(
    UUID id,
    UUID ownerId,
    UUID petId,
    String type,
    Instant startedAt,
    long durationSeconds,
    Double distanceMeters,
    String notes,
    Instant createdAt) {

    public static ActivityResponseDto from(Activity activity) {
        return new ActivityResponseDto(
            activity.getId(), activity.getOwnerId(), activity.getPetId(), activity.getType().name().toLowerCase(),
            activity.getStartedAt(), activity.getDurationSeconds(), activity.getDistanceMeters(), activity.getNotes(),
            activity.getCreatedAt());
    }
}
