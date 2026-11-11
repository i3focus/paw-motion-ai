package com.pawmotionai.social.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityCommentDto(UUID id, UUID activityId, UUID ownerId, String ownerName, String ownerAvatarUrl,
                                 String body, Instant createdAt) {
}
