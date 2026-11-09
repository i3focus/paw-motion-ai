package com.pawmotionai.pet.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdatePetCommand(
    UUID ownerId,
    UUID petId,
    String name,
    String species,
    String breed,
    LocalDate birthDate,
    String size,
    String activityPreference) {
}
