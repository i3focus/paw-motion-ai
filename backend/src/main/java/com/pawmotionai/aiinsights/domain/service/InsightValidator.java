package com.pawmotionai.aiinsights.domain.service;

import java.util.List;
import java.util.Locale;

import com.pawmotionai.aiinsights.domain.exception.InvalidInsightException;

public final class InsightValidator {

    private static final int MAX_TEXT_LENGTH = 1000;
    private static final List<String> MEDICAL_TERMS = List.of(
        "diagnosis", "diagnose", "medical advice", "vet prescription", "treatment", "disease", "symptom");

    private InsightValidator() {
    }

    public static void validate(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidInsightException("Insight text cannot be blank");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidInsightException("Insight text exceeds maximum length");
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String term : MEDICAL_TERMS) {
            if (normalized.contains(term)) {
                throw new InvalidInsightException("Insight contains unsupported medical language");
            }
        }
    }
}
