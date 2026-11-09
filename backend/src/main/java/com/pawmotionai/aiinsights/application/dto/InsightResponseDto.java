package com.pawmotionai.aiinsights.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.aiinsights.domain.model.Insight;

public record InsightResponseDto(
    UUID id,
    UUID ownerId,
    UUID petId,
    String text,
    String provider,
    String model,
    boolean fallbackUsed,
    Instant generatedAt) {

    public static InsightResponseDto from(Insight insight) {
        return new InsightResponseDto(
            insight.getId(), insight.getOwnerId(), insight.getPetId(), insight.getText(), insight.getProvider(),
            insight.getModel(), insight.isFallbackUsed(), insight.getGeneratedAt());
    }
}
