package com.pawmotionai.aiinsights.application.port.out

import com.pawmotionai.aiinsights.domain.model.Insight
import java.util.UUID

interface AiInsightProviderPort {
    fun generate(prompt: InsightPrompt): InsightDraft
}

data class InsightDraft(val text: String, val provider: String, val model: String)

data class InsightPrompt(
    val ownerId: UUID,
    val petId: UUID?,
    val petName: String?,
    val periodDays: Int,
    val activityCount: Int,
    val durationSeconds: Long,
    val distanceMeters: Double,
)

interface InsightRepository {
    fun save(insight: Insight): Insight
}
