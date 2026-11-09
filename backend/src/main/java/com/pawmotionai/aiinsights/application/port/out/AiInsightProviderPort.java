package com.pawmotionai.aiinsights.application.port.out;

public interface AiInsightProviderPort {

    InsightDraft generate(InsightPrompt prompt);
}
