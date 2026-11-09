package com.pawmotionai.aiinsights.adapter.in.rest;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateInsightRequest(UUID petId, @Min(1) @Max(90) Integer periodDays) {
}
