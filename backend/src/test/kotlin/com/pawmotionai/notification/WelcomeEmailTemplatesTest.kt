package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WelcomeEmailTemplatesTest {

    private val properties = MailProperties().apply {
        appBaseUrl = "https://app.i3pet.com.br"
        partnerPortalBaseUrl = "https://app.i3pet.com.br/partner"
        supportEmail = "support@i3pet.com.br"
    }
    private val templates = WelcomeEmailTemplates(properties)

    @Test
    fun rendersTutorEmailWithDisplayNameAndAppLink() {
        val email = templates.tutorEmail("Ana")

        assertEquals("Welcome to Paw Motion AI!", email.subject)
        assertTrue(email.html.contains("Welcome aboard, Ana!"))
        assertTrue(email.html.contains("href=\"https://app.i3pet.com.br\""))
        assertTrue(email.html.contains("support@i3pet.com.br"))
        assertTrue(email.text.contains("Ana"))
        assertTrue(email.text.contains("https://app.i3pet.com.br"))
    }

    @Test
    fun rendersPartnerEmailWithBusinessNameAndPortalLink() {
        val email = templates.partnerEmail("Bruno", "Happy Paws Clinic")

        assertEquals("Welcome to the Paw Motion AI partner program!", email.subject)
        assertTrue(email.html.contains("Welcome, Bruno!"))
        assertTrue(email.html.contains("<strong>Happy Paws Clinic</strong>"))
        assertTrue(email.html.contains("href=\"https://app.i3pet.com.br/partner\""))
        assertTrue(email.text.contains("Happy Paws Clinic"))
        assertTrue(email.text.contains("https://app.i3pet.com.br/partner"))
    }

    @Test
    fun escapesUserControlledValuesInsideTheHtmlTemplate() {
        val email = templates.tutorEmail("<script>alert(1)</script>")

        assertFalse(email.html.contains("<script>alert(1)</script>"))
        assertTrue(email.html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
    }
}
