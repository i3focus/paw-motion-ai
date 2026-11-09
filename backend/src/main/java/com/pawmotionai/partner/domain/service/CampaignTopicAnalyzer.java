package com.pawmotionai.partner.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Component
public class CampaignTopicAnalyzer {

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it", "of", "on", "or",
        "the", "to", "with", "your", "our", "their", "this", "that", "we", "you", "pet", "pets");
    private static final Set<String> CAMPAIGN_TERMS = Set.of(
        "dog", "dogs", "cat", "cats", "puppy", "puppies", "kitten", "kittens", "walk", "walking", "run", "running",
        "activity", "activities", "exercise", "fitness", "health", "healthy", "nutrition", "food", "diet", "grooming",
        "adventure", "outdoor", "indoors", "play", "training", "behavior", "weight", "sleep", "vaccination",
        "wellness", "hydration", "safety", "seasonal", "summer", "winter", "spring", "fall", "rain", "heat",
        "cold", "family", "senior", "breed", "size", "small", "large", "toy", "retriever",
        "shepherd", "poodle", "bulldog", "beagle", "labrador", "chihuahua", "dachshund", "pomeranian");

    public List<String> analyze(PartnerCampaign campaign) {
        List<String> topics = new ArrayList<>();
        addTerms(topics, campaign.getTitle());
        addTerms(topics, campaign.getDescription());
        addTerms(topics, campaign.getTargetAudience());
        addTerms(topics, campaign.getCallToAction());
        if (topics.isEmpty()) {
            topics.add("pet wellness");
            topics.add("healthy habits");
            topics.add("seasonal care");
        }
        return topics.stream()
            .sorted(Comparator.comparingInt(topic -> CAMPAIGN_TERMS.contains(topic) ? 0 : 1))
            .distinct()
            .limit(8)
            .toList();
    }

    private void addTerms(List<String> topics, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        StringBuilder current = new StringBuilder();
        for (int index = 0; index <= normalized.length(); index++) {
            char character = index == normalized.length() ? ' ' : normalized.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '-' || character == '\'') {
                current.append(character);
            } else {
                addWord(topics, current.toString());
                current.setLength(0);
            }
        }
    }

    private void addWord(List<String> topics, String word) {
        if (word.length() < 3 || STOP_WORDS.contains(word)) {
            return;
        }
        String normalized = normalize(word);
        String topic = CAMPAIGN_TERMS.contains(normalized) ? normalized : normalized + " care";
        if (!topics.contains(topic)) {
            topics.add(topic);
        }
    }

    private String normalize(String word) {
        List<String> candidates = new ArrayList<>();
        if (word.endsWith("ies") && word.length() > 4) {
            candidates.add(word.substring(0, word.length() - 3) + "y");
        }
        if (word.endsWith("ing") && word.length() > 5) {
            String base = word.substring(0, word.length() - 3);
            if (base.length() > 1 && base.charAt(base.length() - 1) == base.charAt(base.length() - 2)
                && Character.isLetter(base.charAt(base.length() - 1))) {
                base = base.substring(0, base.length() - 1);
            }
            candidates.add(base);
        }
        if (word.endsWith("s") && word.length() > 3 && !word.endsWith("ss")) {
            candidates.add(word.substring(0, word.length() - 1));
        }
        candidates.add(word);
        return candidates.stream().filter(CAMPAIGN_TERMS::contains).findFirst().orElse(word);
    }
}
