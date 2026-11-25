package com.pawmotionai.aiinsights.application.usecase

import com.pawmotionai.activity.application.port.out.ActivityRepository
import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto
import com.pawmotionai.aiinsights.application.port.`in`.GenerateInsightUseCase
import com.pawmotionai.aiinsights.application.port.out.AiInsightProviderPort
import com.pawmotionai.aiinsights.application.port.out.InsightDraft
import com.pawmotionai.aiinsights.application.port.out.InsightPrompt
import com.pawmotionai.aiinsights.application.port.out.InsightRepository
import com.pawmotionai.aiinsights.domain.model.Insight
import com.pawmotionai.aiinsights.domain.service.InsightValidator
import com.pawmotionai.pet.application.port.`in`.GetPetUseCase
import com.pawmotionai.shared.AiQuota
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@Service
class GenerateInsightService(
    private val activityRepository: ActivityRepository,
    private val getPetUseCase: GetPetUseCase,
    private val insightProvider: AiInsightProviderPort,
    private val insightRepository: InsightRepository,
    private val aiQuota: AiQuota,
) : GenerateInsightUseCase {

    @Transactional
    override fun generateInsight(command: GenerateInsightCommand): InsightResponseDto {
        // Ownership first: a foreign or unknown pet is refused before quota use or any activity scan.
        val petName = command.petId?.let { getPetUseCase.getPet(command.ownerId, it).name }
        aiQuota.consume(command.ownerId)
        val periodDays = command.periodDays
        val from = Instant.now().minus(periodDays.toLong(), ChronoUnit.DAYS)
        val totals = activityRepository.findForInsights(command.ownerId, command.petId, from, MAX_ACTIVITY_ROWS)
        val duration = totals.durationSeconds
        val distance = totals.distanceMeters

        val prompt = InsightPrompt(
            command.ownerId, command.petId, petName, periodDays, totals.count, duration, distance)
        var fallbackUsed = false
        val draft = try {
            insightProvider.generate(prompt).also { InsightValidator.validate(it.text) }
        } catch (exception: Exception) {
            fallbackUsed = true
            fallbackDraft(prompt)
        }

        val insight = Insight.generated(
            UUID.randomUUID(), command.ownerId, command.petId, draft.text, draft.provider, draft.model,
            fallbackUsed, Instant.now())
        return InsightResponseDto.from(insightRepository.save(insight))
    }

    private companion object {
        const val MAX_ACTIVITY_ROWS = 5000
    }

    private fun fallbackDraft(prompt: InsightPrompt): InsightDraft {
        val subject = prompt.petName ?: "your pet"
        val text = if (prompt.activityCount == 0) {
            String.format(Locale.ROOT,
                "No activities were recorded for %s in the last %d days. Start with a short walk to build a consistent routine.",
                subject, prompt.periodDays)
        } else {
            String.format(Locale.ROOT,
                "%s recorded %d activities in the last %d days, totaling %.1f km and %d minutes. Keep the routine consistent and increase distance gradually.",
                capitalize(subject), prompt.activityCount, prompt.periodDays, prompt.distanceMeters / 1000.0,
                prompt.durationSeconds / 60)
        }
        return InsightDraft(text, "deterministic-fallback", "fallback-v1")
    }

    private fun capitalize(value: String): String =
        value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}
