package com.pawmotionai.social.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ActivityPostDto(
    UUID id,
    UUID ownerId,
    String ownerName,
    String ownerAvatarUrl,
    UUID petId,
    String petName,
    String petSpecies,
    String type,
    Instant startedAt,
    long durationSeconds,
    Double distanceMeters,
    String notes,
    Double latitude,
    Double longitude,
    Instant createdAt,
    Map<String, Long> reactions,
    long commentCount,
    String viewerReaction) {
}
