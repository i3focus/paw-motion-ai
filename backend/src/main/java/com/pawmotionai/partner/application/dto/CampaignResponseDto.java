package com.pawmotionai.partner.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pawmotionai.partner.domain.model.PartnerCampaign;

public record CampaignResponseDto(UUID id, UUID partnerId, String title, String description, String targetAudience,
                                  String callToAction, String status, List<String> aiTopics, Instant analyzedAt,
                                  long creditsCharged, long creditsRefunded, Instant expiresAt, Long remainingBalance,
                                  Instant createdAt, Instant updatedAt) {

    public static CampaignResponseDto from(PartnerCampaign campaign) {
        return from(campaign, null);
    }

    public static CampaignResponseDto from(PartnerCampaign campaign, Long remainingBalance) {
        return new CampaignResponseDto(campaign.getId(), campaign.getPartnerId(), campaign.getTitle(),
            campaign.getDescription(), campaign.getTargetAudience(), campaign.getCallToAction(),
            campaign.getStatus().name(), campaign.topics(), campaign.getAnalyzedAt(), campaign.getCreditsCharged(),
            campaign.getCreditsRefunded(), campaign.getExpiresAt(), remainingBalance, campaign.getCreatedAt(),
            campaign.getUpdatedAt());
    }
}
