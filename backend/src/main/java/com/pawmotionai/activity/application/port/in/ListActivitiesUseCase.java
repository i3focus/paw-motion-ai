package com.pawmotionai.activity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;

public interface ListActivitiesUseCase {

    List<ActivityResponseDto> listActivities(UUID ownerId, UUID petId, Instant from, Instant to);
}
