package com.pawmotionai.shared

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlValidationTest {
    @Test
    fun `aceita http e https com host`() {
        assertTrue(UrlValidation.isHttpUrl("https://exemplo.com/a?b=1"))
        assertTrue(UrlValidation.isHttpUrl("HTTP://exemplo.com"))
        assertEquals("https://x.com", UrlValidation.requireHttpUrlOrNull("website", "  https://x.com "))
    }

    @Test
    fun `rejeita esquemas perigosos, host vazio e lixo`() {
        listOf("javascript:alert(1)", "data:text/html,x", "file:///etc/passwd", "intent://x#Intent;end", "https://", "http:///x", "//x.com", "x.com", "https://a b.com")
            .forEach { assertFalse(UrlValidation.isHttpUrl(it), it) }
        assertFailsWith<BadRequestException> { UrlValidation.requireHttpUrlOrNull("website", "javascript:alert(1)") }
    }

    @Test
    fun `nulo ou vazio vira null`() {
        assertNull(UrlValidation.requireHttpUrlOrNull("website", null))
        assertNull(UrlValidation.requireHttpUrlOrNull("website", "   "))
    }
}
