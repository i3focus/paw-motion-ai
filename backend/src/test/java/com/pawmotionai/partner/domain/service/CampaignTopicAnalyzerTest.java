package com.pawmotionai.partner.domain.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.pawmotionai.partner.domain.model.PartnerCampaign;

class CampaignTopicAnalyzerTest {

    private final CampaignTopicAnalyzer analyzer = new CampaignTopicAnalyzer();

    @Test
    void extractsCampaignTopicsForOwnerRecommendations() {
        PartnerCampaign campaign = PartnerCampaign.create(UUID.randomUUID(), UUID.randomUUID(),
            "Summer dog walking challenge", "Encourage daily walks, hydration, and outdoor safety for dogs.",
            "Active dog owners", "Join the 7-day challenge", Instant.now());

        List<String> topics = analyzer.analyze(campaign);

        assertTrue(topics.contains("dog"));
        assertTrue(topics.contains("walk"));
        assertTrue(topics.contains("hydration"));
        assertTrue(topics.contains("safety"));
    }

    @Test
    void fallsBackToWellnessTopicsWhenTextHasNoKnownTerms() {
        PartnerCampaign campaign = PartnerCampaign.create(UUID.randomUUID(), UUID.randomUUID(),
            "The pet", "and or", "we you", "is it", Instant.now());

        List<String> topics = analyzer.analyze(campaign);

        assertTrue(topics.contains("pet wellness"));
        assertTrue(topics.contains("seasonal care"));
    }
}
