package com.pawmotionai.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "app.billing.abacatepay")
@Validated
class AbacatePayProperties {
    var apiKey: String = ""
    var webhookSecret: String = ""
    var creditProductExternalId: String = "pawmotion-credits-brl"
    var baseUrl: String = "https://api.abacatepay.com/v2"
}
