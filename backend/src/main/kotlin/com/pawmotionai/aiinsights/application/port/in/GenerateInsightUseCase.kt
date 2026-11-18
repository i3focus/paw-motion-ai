package com.pawmotionai.aiinsights.application.port.`in`

import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto

interface GenerateInsightUseCase {
    fun generateInsight(command: GenerateInsightCommand): InsightResponseDto
}
