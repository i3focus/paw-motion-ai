package com.pawmotionai.social.application.port.in;

import java.util.UUID;

import com.pawmotionai.social.application.dto.ActivityCommentDto;

public interface AddActivityCommentUseCase {
    ActivityCommentDto addComment(UUID ownerId, UUID activityId, String body);
}
