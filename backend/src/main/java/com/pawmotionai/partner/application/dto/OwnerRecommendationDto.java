package com.pawmotionai.partner.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pawmotionai.partner.domain.model.PartnerCampaign;

public record OwnerRecommendationDto(UUID campaignId, String partnerName, String campaignTitle, String description,
                                     String callToAction, List<String> recommendedTopics, Instant analyzedAt) {

    public static OwnerRecommendationDto from(PartnerCampaign campaign, String partnerName) {
        return new OwnerRecommendationDto(campaign.getId(), partnerName, campaign.getTitle(), campaign.getDescription(),
            campaign.getCallToAction(), campaign.topics(), campaign.getAnalyzedAt());
    }
}
