package com.pawmotionai.aiinsights.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.port.in.ListActivitiesUseCase;
import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand;
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto;
import com.pawmotionai.aiinsights.application.port.in.GenerateInsightUseCase;
import com.pawmotionai.aiinsights.application.port.out.AiInsightProviderPort;
import com.pawmotionai.aiinsights.application.port.out.InsightDraft;
import com.pawmotionai.aiinsights.application.port.out.InsightPrompt;
import com.pawmotionai.aiinsights.application.port.out.InsightRepository;
import com.pawmotionai.aiinsights.domain.exception.InvalidInsightException;
import com.pawmotionai.aiinsights.domain.model.Insight;
import com.pawmotionai.aiinsights.domain.service.InsightValidator;
import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.GetPetUseCase;

@Service
public class GenerateInsightService implements GenerateInsightUseCase {

    private final ListActivitiesUseCase listActivitiesUseCase;
    private final GetPetUseCase getPetUseCase;
    private final AiInsightProviderPort insightProvider;
    private final InsightRepository insightRepository;

    public GenerateInsightService(ListActivitiesUseCase listActivitiesUseCase, GetPetUseCase getPetUseCase,
                                  AiInsightProviderPort insightProvider, InsightRepository insightRepository) {
        this.listActivitiesUseCase = listActivitiesUseCase;
        this.getPetUseCase = getPetUseCase;
        this.insightProvider = insightProvider;
        this.insightRepository = insightRepository;
    }

    @Override
    @Transactional
    public InsightResponseDto generateInsight(GenerateInsightCommand command) {
        int periodDays = command.periodDays();
        Instant from = Instant.now().minus(periodDays, ChronoUnit.DAYS);
        List<ActivityResponseDto> activities = listActivitiesUseCase.listActivities(command.ownerId(), command.petId(), from, null);

        String petName = null;
        if (command.petId() != null) {
            PetResponseDto pet = getPetUseCase.getPet(command.ownerId(), command.petId());
            petName = pet.name();
        }

        long duration = activities.stream().mapToLong(ActivityResponseDto::durationSeconds).sum();
        double distance = activities.stream()
            .mapToDouble(activity -> activity.distanceMeters() == null ? 0.0 : activity.distanceMeters())
            .sum();

        InsightPrompt prompt = new InsightPrompt(
            command.ownerId(), command.petId(), petName, periodDays, activities.size(), duration, distance);
        InsightDraft draft;
        boolean fallbackUsed = false;
        try {
            draft = insightProvider.generate(prompt);
            InsightValidator.validate(draft.text());
        } catch (Exception exception) {
            fallbackUsed = true;
            draft = fallbackDraft(prompt);
        }

        Insight insight = Insight.generated(
            UUID.randomUUID(), command.ownerId(), command.petId(), draft.text(), draft.provider(), draft.model(),
            fallbackUsed, Instant.now());
        return InsightResponseDto.from(insightRepository.save(insight));
    }

    private InsightDraft fallbackDraft(InsightPrompt prompt) {
        String subject = prompt.petName() == null ? "your pet" : prompt.petName();
        String text;
        if (prompt.activityCount() == 0) {
            text = String.format(Locale.ROOT,
                "No activities were recorded for %s in the last %d days. Start with a short walk to build a consistent routine.",
                subject, prompt.periodDays());
        } else {
            text = String.format(Locale.ROOT,
                "%s recorded %d activities in the last %d days, totaling %.1f km and %d minutes. Keep the routine consistent and increase distance gradually.",
                capitalize(subject), prompt.activityCount(), prompt.periodDays(), prompt.distanceMeters() / 1000.0,
                prompt.durationSeconds() / 60);
        }
        return new InsightDraft(text, "deterministic-fallback", "fallback-v1");
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
