package com.pawmotionai.assistant

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class AiActionType { BOOKING, REMINDER, CAPTION, PLAYDATE }
enum class AiActionStatus { PROPOSED, CONFIRMED, EXECUTED, FAILED, CANCELLED }

data class CreateConversationRequest(val petId: UUID?)

data class SendMessageRequest(@field:NotBlank @field:Size(max = 2000) val content: String?)

data class ConversationDto(val id: UUID, val petId: UUID?, val title: String, val updatedAt: Instant)

data class AiActionDto(
    val id: UUID,
    val type: AiActionType,
    val status: AiActionStatus,
    val payload: Map<String, Any?>,
    val sponsored: Boolean,
    val campaignId: UUID?,
    val resultRef: UUID?,
    val error: String?,
)

data class AiMessageDto(
    val id: UUID,
    val role: String,
    val content: String,
    val createdAt: Instant,
    val action: AiActionDto?,
)

data class SendMessageResponse(val userMessage: AiMessageDto, val assistantMessage: AiMessageDto, val usage: AiUsageDto)

data class AiUsageDto(val plan: String, val used: Int, val limit: Int?)
