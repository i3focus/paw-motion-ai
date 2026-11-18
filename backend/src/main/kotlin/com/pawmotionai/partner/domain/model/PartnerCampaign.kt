package com.pawmotionai.partner.domain.model

import com.pawmotionai.partner.domain.exception.CampaignStateException
import java.time.Instant
import java.util.UUID

enum class CampaignEndReason {
    ARCHIVED,
    EXPIRED,
    ENDED_BY_PARTNER,
    BUDGET_EXHAUSTED
}

enum class CampaignStatus {
    DRAFT,
    IN_REVIEW,
    ACTIVE,
    PAUSED,
    ENDED;

    fun isVisibleToOwners(): Boolean = this == ACTIVE

    companion object {
        fun fromValue(value: String?): CampaignStatus {
            if (value.isNullOrBlank()) {
                return DRAFT
            }
            val normalized = value.trim().uppercase()
            return if (normalized == "ARCHIVED" || normalized == "EXPIRED") ENDED else valueOf(normalized)
        }
    }
}

class PartnerCampaign private constructor(
    val id: UUID,
    val partnerId: UUID,
    val title: String,
    val description: String,
    val targetAudience: String,
    val callToAction: String,
    var status: CampaignStatus,
    var aiTopics: String?,
    var analyzedAt: Instant?,
    val creditsCharged: Long,
    val creditsRefunded: Long,
    val expiresAt: Instant?,
    val idempotencyKey: String?,
    val createdAt: Instant,
    var updatedAt: Instant,
    var endReason: CampaignEndReason?,
) {

    fun belongsTo(accountId: UUID): Boolean = partnerId == accountId

    fun expire(now: Instant) {
        if (status == CampaignStatus.ENDED) {
            throw CampaignStateException("Campaign cannot expire from status $status")
        }
        if (expiresAt == null || now.isBefore(expiresAt)) {
            throw CampaignStateException("Campaign has not reached its expiration date")
        }
        status = CampaignStatus.ENDED
        endReason = CampaignEndReason.EXPIRED
    }

    fun topics(): List<String> =
        aiTopics?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun isArchived(): Boolean = status == CampaignStatus.ENDED && endReason == CampaignEndReason.ARCHIVED

    fun restoreEndReason(endReason: CampaignEndReason?) {
        this.endReason = endReason
    }

    companion object {
        fun create(id: UUID, partnerId: UUID, title: String, description: String, targetAudience: String,
                   callToAction: String, createdAt: Instant): PartnerCampaign =
            PartnerCampaign(id, partnerId, title.trim(), description.trim(), targetAudience.trim(),
                callToAction.trim(), CampaignStatus.DRAFT, null, null, 0, 0, null, null, createdAt, createdAt, null)

        fun createCharged(id: UUID, partnerId: UUID, title: String, description: String, targetAudience: String,
                          callToAction: String, creditsCharged: Long, expiresAt: Instant?, idempotencyKey: String?,
                          createdAt: Instant): PartnerCampaign =
            PartnerCampaign(id, partnerId, title.trim(), description.trim(), targetAudience.trim(),
                callToAction.trim(), CampaignStatus.DRAFT, null, null, creditsCharged, 0, expiresAt, idempotencyKey,
                createdAt, createdAt, null)

        fun reconstitute(id: UUID, partnerId: UUID, title: String, description: String, targetAudience: String,
                         callToAction: String, status: CampaignStatus, aiTopics: String?, analyzedAt: Instant?,
                         createdAt: Instant, updatedAt: Instant): PartnerCampaign =
            reconstitute(id, partnerId, title, description, targetAudience, callToAction, status, aiTopics,
                analyzedAt, 0, 0, null, null, createdAt, updatedAt)

        fun reconstitute(id: UUID, partnerId: UUID, title: String, description: String, targetAudience: String,
                         callToAction: String, status: CampaignStatus, aiTopics: String?, analyzedAt: Instant?,
                         creditsCharged: Long, creditsRefunded: Long, expiresAt: Instant?, idempotencyKey: String?,
                         createdAt: Instant, updatedAt: Instant): PartnerCampaign =
            PartnerCampaign(id, partnerId, title, description, targetAudience, callToAction, status, aiTopics,
                analyzedAt, creditsCharged, creditsRefunded, expiresAt, idempotencyKey, createdAt, updatedAt, null)
    }
}
