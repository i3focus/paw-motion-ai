package com.pawmotionai.aiinsights.adapter.out.external;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.pawmotionai.aiinsights.application.port.out.AiInsightProviderPort;
import com.pawmotionai.aiinsights.application.port.out.InsightDraft;
import com.pawmotionai.aiinsights.application.port.out.InsightPrompt;

@Component
public class LocalRuleBasedInsightProvider implements AiInsightProviderPort {

    @Override
    public InsightDraft generate(InsightPrompt prompt) {
        String subject = prompt.petName() == null ? "your pet" : prompt.petName();
        String text;
        if (prompt.activityCount() == 0) {
            text = String.format(Locale.ROOT,
                "%s has no recorded activities in the last %d days. A short daily walk is a simple way to start building consistency.",
                capitalize(subject), prompt.periodDays());
        } else {
            text = String.format(Locale.ROOT,
                "%s completed %d activities in the last %d days with %.1f km and %d minutes of activity. The routine is active; consider keeping the same days each week.",
                capitalize(subject), prompt.activityCount(), prompt.periodDays(), prompt.distanceMeters() / 1000.0,
                prompt.durationSeconds() / 60);
        }
        return new InsightDraft(text, "local-rules", "deterministic-v1");
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
