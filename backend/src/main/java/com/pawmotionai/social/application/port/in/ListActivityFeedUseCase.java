package com.pawmotionai.social.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.social.application.dto.ActivityPostDto;

public interface ListActivityFeedUseCase {
    List<ActivityPostDto> listFeed(UUID viewerId, int page, int size);
}
