package com.pawmotionai.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "app.mail")
@Validated
class MailProperties {
    var enabled: Boolean = false
    var from: String = "Paw Motion AI <onboarding@resend.dev>"
    var appBaseUrl: String = "http://localhost:5173"
    var partnerPortalBaseUrl: String = "http://localhost:5173/partner"
    var supportEmail: String = "support@i3pet.com.br"
    var resend: Resend = Resend()

    class Resend {
        var apiKey: String = ""
        var baseUrl: String = "https://api.resend.com"
        var connectTimeoutMs: Long = 3000
        var readTimeoutMs: Long = 8000
    }
}
