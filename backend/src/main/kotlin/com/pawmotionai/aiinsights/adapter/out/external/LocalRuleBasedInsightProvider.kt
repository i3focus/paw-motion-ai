package com.pawmotionai.aiinsights.adapter.out.external

import com.pawmotionai.aiinsights.application.port.out.AiInsightProviderPort
import com.pawmotionai.aiinsights.application.port.out.InsightDraft
import com.pawmotionai.aiinsights.application.port.out.InsightPrompt
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class LocalRuleBasedInsightProvider : AiInsightProviderPort {

    override fun generate(prompt: InsightPrompt): InsightDraft {
        val subject = prompt.petName ?: "your pet"
        val text = if (prompt.activityCount == 0) {
            String.format(Locale.ROOT,
                "%s has no recorded activities in the last %d days. A short daily walk is a simple way to start building consistency.",
                capitalize(subject), prompt.periodDays)
        } else {
            String.format(Locale.ROOT,
                "%s completed %d activities in the last %d days with %.1f km and %d minutes of activity. The routine is active; consider keeping the same days each week.",
                capitalize(subject), prompt.activityCount, prompt.periodDays, prompt.distanceMeters / 1000.0,
                prompt.durationSeconds / 60)
        }
        return InsightDraft(text, "local-rules", "deterministic-v1")
    }

    private fun capitalize(value: String): String =
        value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}
