package com.pawmotionai.social.application.port.in;

import java.util.UUID;

import com.pawmotionai.social.application.dto.ActivityPostDto;

public interface ToggleReactionUseCase {
    ActivityPostDto toggleReaction(UUID ownerId, UUID activityId, String type);
}
