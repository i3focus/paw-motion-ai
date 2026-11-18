package com.pawmotionai.partner.domain.service

import com.pawmotionai.partner.domain.model.PartnerCampaign
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class CampaignTopicAnalyzer {

    fun analyze(campaign: PartnerCampaign): List<String> {
        val topics = mutableListOf<String>()
        addTerms(topics, campaign.title)
        addTerms(topics, campaign.description)
        addTerms(topics, campaign.targetAudience)
        addTerms(topics, campaign.callToAction)
        if (topics.isEmpty()) {
            topics.add("pet wellness")
            topics.add("healthy habits")
            topics.add("seasonal care")
        }
        return topics
            .sortedBy { if (CAMPAIGN_TERMS.contains(it)) 0 else 1 }
            .distinct()
            .take(8)
    }

    private fun addTerms(topics: MutableList<String>, text: String?) {
        if (text.isNullOrBlank()) {
            return
        }
        val normalized = text.lowercase(Locale.ROOT)
        val current = StringBuilder()
        for (index in 0..normalized.length) {
            val character = if (index == normalized.length) ' ' else normalized[index]
            if (character.isLetterOrDigit() || character == '-' || character == '\'') {
                current.append(character)
            } else {
                addWord(topics, current.toString())
                current.setLength(0)
            }
        }
    }

    private fun addWord(topics: MutableList<String>, word: String) {
        if (word.length < 3 || STOP_WORDS.contains(word)) {
            return
        }
        val normalized = normalize(word)
        val topic = if (CAMPAIGN_TERMS.contains(normalized)) normalized else "${normalized} care"
        if (!topics.contains(topic)) {
            topics.add(topic)
        }
    }

    private fun normalize(word: String): String {
        val candidates = mutableListOf<String>()
        if (word.endsWith("ies") && word.length > 4) {
            candidates.add(word.substring(0, word.length - 3) + "y")
        }
        if (word.endsWith("ing") && word.length > 5) {
            var base = word.substring(0, word.length - 3)
            if (base.length > 1 && base[base.length - 1] == base[base.length - 2]
                && base[base.length - 1].isLetter()) {
                base = base.substring(0, base.length - 1)
            }
            candidates.add(base)
        }
        if (word.endsWith("s") && word.length > 3 && !word.endsWith("ss")) {
            candidates.add(word.substring(0, word.length - 1))
        }
        candidates.add(word)
        return candidates.firstOrNull(CAMPAIGN_TERMS::contains) ?: word
    }

    private companion object {
        val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it", "of", "on", "or",
            "the", "to", "with", "your", "our", "their", "this", "that", "we", "you", "pet", "pets")
        val CAMPAIGN_TERMS = setOf(
            "dog", "dogs", "cat", "cats", "puppy", "puppies", "kitten", "kittens", "walk", "walking", "run", "running",
            "activity", "activities", "exercise", "fitness", "health", "healthy", "nutrition", "food", "diet", "grooming",
            "adventure", "outdoor", "indoors", "play", "training", "behavior", "weight", "sleep", "vaccination",
            "wellness", "hydration", "safety", "seasonal", "summer", "winter", "spring", "fall", "rain", "heat",
            "cold", "family", "senior", "breed", "size", "small", "large", "toy", "retriever",
            "shepherd", "poodle", "bulldog", "beagle", "labrador", "chihuahua", "dachshund", "pomeranian")
    }
}
