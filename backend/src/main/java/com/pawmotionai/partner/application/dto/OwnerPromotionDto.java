package com.pawmotionai.partner.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.partner.domain.model.PartnerCampaign;

public record OwnerPromotionDto(UUID campaignId, String partnerName, String partnerWebsite, String title,
                                String description, String targetAudience, String callToAction,
                                Instant expiresAt, Instant createdAt) {

    public static OwnerPromotionDto from(PartnerCampaign campaign, String partnerName, String partnerWebsite) {
        return new OwnerPromotionDto(campaign.getId(), partnerName, partnerWebsite, campaign.getTitle(),
            campaign.getDescription(), campaign.getTargetAudience(), campaign.getCallToAction(),
            campaign.getExpiresAt(), campaign.getCreatedAt());
    }
}
