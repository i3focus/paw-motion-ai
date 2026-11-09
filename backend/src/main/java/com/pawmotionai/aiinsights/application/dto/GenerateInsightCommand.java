package com.pawmotionai.aiinsights.application.dto;

import java.util.UUID;

public record GenerateInsightCommand(UUID ownerId, UUID petId, int periodDays) {
}
