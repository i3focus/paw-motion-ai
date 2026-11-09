package com.pawmotionai.aiinsights.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.pawmotionai.aiinsights.domain.exception.InvalidInsightException;

class InsightValidatorTest {

    @Test
    void rejectsBlankText() {
        assertThrows(InvalidInsightException.class, () -> InsightValidator.validate(" "));
    }

    @Test
    void rejectsMedicalLanguage() {
        assertThrows(InvalidInsightException.class,
            () -> InsightValidator.validate("This is a diagnosis and treatment plan."));
    }

    @Test
    void acceptsActivitySummary() {
        assertDoesNotThrow(() -> InsightValidator.validate("Your pet completed 3 walks this week."));
    }
}
