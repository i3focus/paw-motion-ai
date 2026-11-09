package com.pawmotionai.pet.adapter.in.rest;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreatePetRequest(
    @NotBlank @Size(max = 80) String name,
    @NotBlank String species,
    @Size(max = 80) String breed,
    @PastOrPresent LocalDate birthDate,
    @NotBlank String size,
    @Size(max = 120) String activityPreference) {
}
