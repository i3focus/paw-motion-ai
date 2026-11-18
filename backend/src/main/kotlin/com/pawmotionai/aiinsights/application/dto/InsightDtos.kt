package com.pawmotionai.aiinsights.application.dto

import com.pawmotionai.aiinsights.domain.model.Insight
import java.time.Instant
import java.util.UUID

data class GenerateInsightCommand(val ownerId: UUID, val petId: UUID?, val periodDays: Int)

data class InsightResponseDto(
    val id: UUID,
    val ownerId: UUID,
    val petId: UUID?,
    val text: String,
    val provider: String,
    val model: String?,
    val fallbackUsed: Boolean,
    val generatedAt: Instant,
) {

    companion object {
        fun from(insight: Insight): InsightResponseDto =
            InsightResponseDto(
                insight.id, insight.ownerId, insight.petId, insight.text, insight.provider,
                insight.model, insight.fallbackUsed, insight.generatedAt)
    }
}
