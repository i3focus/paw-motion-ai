package com.pawmotionai.social.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.social.application.dto.ActivityCommentDto;

public interface ListActivityCommentsUseCase {
    List<ActivityCommentDto> listComments(UUID activityId);
}
