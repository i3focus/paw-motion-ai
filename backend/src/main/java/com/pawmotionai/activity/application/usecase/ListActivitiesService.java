package com.pawmotionai.activity.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.port.in.ListActivitiesUseCase;
import com.pawmotionai.activity.application.port.out.ActivityRepository;
import com.pawmotionai.activity.domain.model.Activity;

@Service
public class ListActivitiesService implements ListActivitiesUseCase {

    private final ActivityRepository activityRepository;

    public ListActivitiesService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDto> listActivities(UUID ownerId, UUID petId, Instant from, Instant to) {
        return activityRepository.findByOwnerIdOrderByStartedAtDesc(ownerId).stream()
            .filter(activity -> petId == null || activity.getPetId().equals(petId))
            .filter(activity -> from == null || !activity.getStartedAt().isBefore(from))
            .filter(activity -> to == null || !activity.getStartedAt().isAfter(to))
            .map(ActivityResponseDto::from)
            .toList();
    }
}
