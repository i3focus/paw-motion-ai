package com.pawmotionai.aiinsights.domain.service

import com.pawmotionai.aiinsights.domain.exception.InvalidInsightException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InsightValidatorTest {

    @Test
    fun rejectsBlankText() {
        assertThrows(InvalidInsightException::class.java) { InsightValidator.validate(" ") }
    }

    @Test
    fun rejectsMedicalLanguage() {
        assertThrows(InvalidInsightException::class.java) {
            InsightValidator.validate("This is a diagnosis and treatment plan.")
        }
    }

    @Test
    fun acceptsActivitySummary() {
        assertDoesNotThrow { InsightValidator.validate("Your pet completed 3 walks this week.") }
    }
}
