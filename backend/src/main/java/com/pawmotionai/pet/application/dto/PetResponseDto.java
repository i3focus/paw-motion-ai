package com.pawmotionai.pet.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.pawmotionai.pet.domain.model.Pet;

public record PetResponseDto(
    UUID id,
    UUID ownerId,
    String name,
    String species,
    String breed,
    LocalDate birthDate,
    String size,
    String activityPreference,
    String status,
    Instant createdAt) {

    public static PetResponseDto from(Pet pet) {
        return new PetResponseDto(
            pet.getId(), pet.getOwnerId(), pet.getName(), pet.getSpecies().name().toLowerCase(), pet.getBreed(),
            pet.getBirthDate(), pet.getSize().name().toLowerCase(), pet.getActivityPreference(),
            pet.getStatus().name().toLowerCase(), pet.getCreatedAt());
    }
}
