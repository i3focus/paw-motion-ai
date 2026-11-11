package com.pawmotionai.assistant

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

data class ServiceOffer(
    val serviceId: UUID,
    val serviceName: String,
    val partnerName: String,
    val priceCents: Long,
    val sponsored: Boolean,
    val campaignId: UUID?,
)

data class AssistantContext(
    val petId: UUID?,
    val petName: String?,
    val species: String?,
    val message: String,
    val offers: List<ServiceOffer>,
    val today: LocalDate = LocalDate.now(ZoneOffset.UTC),
)

data class ActionProposal(val type: AiActionType, val payload: Map<String, Any?>, val sponsored: Boolean = false, val campaignId: UUID? = null)

data class AssistantReply(val text: String, val provider: String, val model: String, val proposal: ActionProposal?)

enum class AssistantIntent { BOOKING, REMINDER, CAPTION, PLAYDATE, GENERAL }

interface AssistantProviderPort {
    fun reply(context: AssistantContext): AssistantReply
}

/**
 * Deterministic assistant used when no LLM is configured (D-009). It never executes anything: every action is a
 * proposal the owner must confirm.
 */
@Component
class LocalAssistantProvider : AssistantProviderPort {

    companion object {
        private val INTENTS = linkedMapOf(
            AssistantIntent.CAPTION to listOf("caption", "legenda"),
            AssistantIntent.REMINDER to listOf("remind", "vaccine", "vaccination", "deworm", "vacina", "lembr"),
            AssistantIntent.PLAYDATE to listOf("playdate", "play date", "friends to play", "brincar"),
            AssistantIntent.BOOKING to listOf("book", "groom", "bath", "appointment", "vet", "banho", "tosa", "agendar"),
        )

        fun intent(message: String): AssistantIntent {
            val text = message.lowercase(Locale.ROOT)
            return INTENTS.entries.firstOrNull { (_, words) -> words.any { it in text } }?.key ?: AssistantIntent.GENERAL
        }
    }

    override fun reply(context: AssistantContext): AssistantReply {
        val pet = context.petName ?: "your pet"
        val (text, proposal) = when (intent(context.message)) {
            AssistantIntent.BOOKING -> context.offers.firstOrNull()?.let { offer ->
                val label = if (offer.sponsored) " (Sponsored)" else ""
                val startsAt = context.today.plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC)
                "I found ${offer.serviceName} at ${offer.partnerName}$label for ${formatPrice(offer.priceCents)}. " +
                    "Want me to request tomorrow at 10:00 for $pet?" to ActionProposal(
                    AiActionType.BOOKING,
                    mapOf("serviceId" to offer.serviceId.toString(), "petId" to context.petId?.toString(), "startsAt" to startsAt.toString(),
                        "serviceName" to offer.serviceName, "partnerName" to offer.partnerName, "priceCents" to offer.priceCents),
                    offer.sponsored, offer.campaignId,
                )
            } ?: ("I couldn't find a bookable service nearby yet. Try the map to explore places around you." to null)
            AssistantIntent.REMINDER -> {
                val due = context.today.plusDays(14)
                "I can add a vaccine booster reminder for $pet on $due. You'll get a notice a week before." to ActionProposal(
                    AiActionType.REMINDER,
                    mapOf("petId" to context.petId?.toString(), "type" to "VACCINE", "name" to "Vaccine booster", "dueOn" to due.toString()),
                )
            }
            AssistantIntent.CAPTION -> {
                val caption = "Another great day with $pet 🐾 #petlife"
                "Here's a caption idea: \"$caption\". Use it on your next post?" to ActionProposal(AiActionType.CAPTION, mapOf("caption" to caption))
            }
            AssistantIntent.PLAYDATE -> "I can open a playdate for $pet right now so nearby friends can join. Shall I?" to ActionProposal(
                AiActionType.PLAYDATE, mapOf("petId" to context.petId?.toString(), "mode" to "NOW", "message" to "$pet is up for a playdate!"),
            )
            AssistantIntent.GENERAL -> "I can help $pet with bookings, health reminders, post captions and playdates. " +
                "Short daily walks and fresh water are a great baseline for ${context.species ?: "any pet"}." to null
        }
        return AssistantReply(text, "local-rules", "petlife-assistant-v1", proposal?.takeIf { it.type == AiActionType.CAPTION || context.petId != null })
    }

    private fun formatPrice(cents: Long) = "R$ %d,%02d".format(cents / 100, cents % 100)
}
