package com.pawmotionai.aiinsights.application.port.in;

import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand;
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto;

public interface GenerateInsightUseCase {

    InsightResponseDto generateInsight(GenerateInsightCommand command);
}
