package com.pawmotionai.aiinsights.domain.model

import java.time.Instant
import java.util.UUID

class Insight private constructor(
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
        fun generated(id: UUID, ownerId: UUID, petId: UUID?, text: String, provider: String, model: String?,
                      fallbackUsed: Boolean, generatedAt: Instant): Insight =
            Insight(id, ownerId, petId, text, provider, model, fallbackUsed, generatedAt)

        fun reconstitute(id: UUID, ownerId: UUID, petId: UUID?, text: String, provider: String, model: String?,
                         fallbackUsed: Boolean, generatedAt: Instant): Insight =
            Insight(id, ownerId, petId, text, provider, model, fallbackUsed, generatedAt)
    }
}
