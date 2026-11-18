package com.pawmotionai.partner.application.dto

import com.pawmotionai.partner.domain.model.PartnerCampaign
import java.time.Instant
import java.util.UUID

data class CampaignResponseDto(
    val id: UUID,
    val partnerId: UUID,
    val title: String,
    val description: String,
    val targetAudience: String,
    val callToAction: String,
    val status: String,
    val aiTopics: List<String>,
    val analyzedAt: Instant?,
    val creditsCharged: Long,
    val creditsRefunded: Long,
    val expiresAt: Instant?,
    val remainingBalance: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val endReason: String?,
) {

    companion object {
        fun from(campaign: PartnerCampaign): CampaignResponseDto = from(campaign, null)

        fun from(campaign: PartnerCampaign, remainingBalance: Long?): CampaignResponseDto =
            CampaignResponseDto(
                campaign.id, campaign.partnerId, campaign.title, campaign.description, campaign.targetAudience,
                campaign.callToAction, campaign.status.name, campaign.topics(), campaign.analyzedAt,
                campaign.creditsCharged, campaign.creditsRefunded, campaign.expiresAt, remainingBalance,
                campaign.createdAt, campaign.updatedAt, campaign.endReason?.name)
    }
}

data class CreateCampaignCommand(
    val partnerId: UUID,
    val title: String,
    val description: String,
    val targetAudience: String,
    val callToAction: String,
    val idempotencyKey: String?,
)
