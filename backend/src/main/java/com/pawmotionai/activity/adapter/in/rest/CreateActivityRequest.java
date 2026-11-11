package com.pawmotionai.activity.adapter.in.rest;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateActivityRequest(
    @NotNull UUID petId,
    @NotBlank String type,
    @NotNull Instant startedAt,
    @Min(1) long durationSeconds,
    @DecimalMin("0") Double distanceMeters,
    @Size(max = 500) String notes,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude) {
}
