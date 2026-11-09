package com.pawmotionai.activity.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.dto.RecordActivityCommand;
import com.pawmotionai.activity.application.port.in.RecordActivityUseCase;
import com.pawmotionai.activity.application.port.out.ActivityRepository;
import com.pawmotionai.activity.domain.exception.InvalidActivityException;
import com.pawmotionai.activity.domain.model.Activity;
import com.pawmotionai.activity.domain.model.ActivityType;
import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.GetPetUseCase;

@Service
public class RecordActivityService implements RecordActivityUseCase {

    private final ActivityRepository activityRepository;
    private final GetPetUseCase getPetUseCase;

    public RecordActivityService(ActivityRepository activityRepository, GetPetUseCase getPetUseCase) {
        this.activityRepository = activityRepository;
        this.getPetUseCase = getPetUseCase;
    }

    @Override
    @Transactional
    public ActivityResponseDto recordActivity(RecordActivityCommand command) {
        PetResponseDto pet = getPetUseCase.getPet(command.ownerId(), command.petId());
        if (!"active".equals(pet.status())) {
            throw new InvalidActivityException("Cannot record activity for an inactive pet");
        }

        Activity activity = Activity.record(
            UUID.randomUUID(),
            command.ownerId(),
            command.petId(),
            parseType(command.type()),
            command.startedAt(),
            command.durationSeconds(),
            command.distanceMeters(),
            trimToNull(command.notes()),
            Instant.now());
        return ActivityResponseDto.from(activityRepository.save(activity));
    }

    private ActivityType parseType(String value) {
        try {
            return ActivityType.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidActivityException("Unsupported activity type: " + value);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
