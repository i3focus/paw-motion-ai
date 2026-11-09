package com.pawmotionai.activity.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.dto.DashboardSummaryDto;
import com.pawmotionai.activity.application.port.in.GetDashboardSummaryUseCase;
import com.pawmotionai.activity.application.port.out.ActivityRepository;
import com.pawmotionai.activity.domain.model.Activity;
import com.pawmotionai.activity.domain.service.ActivityStatisticsCalculator;
import com.pawmotionai.pet.application.port.in.ListPetsUseCase;

@Service
public class GetDashboardSummaryService implements GetDashboardSummaryUseCase {

    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final ActivityRepository activityRepository;
    private final ListPetsUseCase listPetsUseCase;

    public GetDashboardSummaryService(ActivityRepository activityRepository, ListPetsUseCase listPetsUseCase) {
        this.activityRepository = activityRepository;
        this.listPetsUseCase = listPetsUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(UUID ownerId) {
        Instant now = Instant.now();
        Instant weekStart = now.minus(7, ChronoUnit.DAYS);
        List<Activity> activities = activityRepository.findByOwnerIdOrderByStartedAtDesc(ownerId);
        List<Activity> last7Days = activities.stream()
            .filter(activity -> !activity.getStartedAt().isBefore(weekStart))
            .toList();

        double distance = last7Days.stream()
            .mapToDouble(activity -> activity.getDistanceMeters() == null ? 0.0 : activity.getDistanceMeters())
            .sum();
        long duration = last7Days.stream().mapToLong(Activity::getDurationSeconds).sum();
        int streak = ActivityStatisticsCalculator.currentStreakDays(activities, now);
        List<ActivityResponseDto> recent = activities.stream()
            .limit(RECENT_ACTIVITY_LIMIT)
            .map(ActivityResponseDto::from)
            .toList();

        return new DashboardSummaryDto(
            ownerId,
            listPetsUseCase.listPets(ownerId).size(),
            last7Days.size(),
            distance,
            duration,
            streak,
            recent);
    }
}
