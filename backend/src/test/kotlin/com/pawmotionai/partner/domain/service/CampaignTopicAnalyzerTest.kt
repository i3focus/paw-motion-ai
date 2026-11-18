package com.pawmotionai.partner.domain.service

import com.pawmotionai.partner.domain.model.PartnerCampaign
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CampaignTopicAnalyzerTest {

    private val analyzer = CampaignTopicAnalyzer()

    @Test
    fun extractsCampaignTopicsForOwnerRecommendations() {
        val campaign = PartnerCampaign.create(UUID.randomUUID(), UUID.randomUUID(),
            "Summer dog walking challenge", "Encourage daily walks, hydration, and outdoor safety for dogs.",
            "Active dog owners", "Join the 7-day challenge", Instant.now())

        val topics = analyzer.analyze(campaign)

        assertTrue(topics.contains("dog"))
        assertTrue(topics.contains("walk"))
        assertTrue(topics.contains("hydration"))
        assertTrue(topics.contains("safety"))
    }

    @Test
    fun fallsBackToWellnessTopicsWhenTextHasNoKnownTerms() {
        val campaign = PartnerCampaign.create(UUID.randomUUID(), UUID.randomUUID(),
            "The pet", "and or", "we you", "is it", Instant.now())

        val topics = analyzer.analyze(campaign)

        assertTrue(topics.contains("pet wellness"))
        assertTrue(topics.contains("seasonal care"))
    }

    @Test
    fun keepsCommonWellnessTermsWellFormed() {
        val campaign = PartnerCampaign.create(UUID.randomUUID(), UUID.randomUUID(),
            "Fitness and wellness running plan", "Improve fitness, wellness, and running routines.",
            "Active owners", "Start today", Instant.now())

        val topics = analyzer.analyze(campaign)

        assertTrue(topics.contains("fitness"))
        assertTrue(topics.contains("wellness"))
        assertTrue(topics.any { it == "run" || it == "running" })
        assertTrue(topics.none { it.contains("fitnes care")
            || it.contains("wellnes care") || it.contains("runn care") })
    }
}
