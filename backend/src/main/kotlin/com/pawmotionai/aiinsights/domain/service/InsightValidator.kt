package com.pawmotionai.aiinsights.domain.service

import com.pawmotionai.aiinsights.domain.exception.InvalidInsightException
import java.util.Locale

object InsightValidator {

    private const val MAX_TEXT_LENGTH = 1000
    private val MEDICAL_TERMS = listOf(
        "diagnosis", "diagnose", "medical advice", "vet prescription", "treatment", "disease", "symptom")

    fun validate(text: String?) {
        if (text.isNullOrBlank()) {
            throw InvalidInsightException("Insight text cannot be blank")
        }
        if (text.length > MAX_TEXT_LENGTH) {
            throw InvalidInsightException("Insight text exceeds maximum length")
        }
        val normalized = text.lowercase(Locale.ROOT)
        for (term in MEDICAL_TERMS) {
            if (normalized.contains(term)) {
                throw InvalidInsightException("Insight contains unsupported medical language")
            }
        }
    }
}
