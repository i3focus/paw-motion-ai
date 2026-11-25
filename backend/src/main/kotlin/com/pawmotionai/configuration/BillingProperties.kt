package com.pawmotionai.configuration

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "app.billing")
@Validated
class BillingProperties {

    @field:NotBlank
    var currency: String = "BRL"

    @field:NotNull
    @field:DecimalMin(value = "0.000001", inclusive = false)
    var creditsPerCurrencyUnit: BigDecimal = BigDecimal.ONE

    @field:NotNull
    @field:DecimalMin(value = "0.01", inclusive = false)
    var minPurchaseAmount: BigDecimal = BigDecimal("10.00")

    @field:NotNull
    @field:DecimalMin(value = "0.01", inclusive = false)
    var maxPurchaseAmount: BigDecimal = BigDecimal("10000.00")

    @field:NotBlank
    var provider: String = ""

    @field:NotNull
    var pendingTtl: Duration = Duration.ofHours(24)

    @field:NotBlank
    var partnerPortalBaseUrl: String = "http://localhost:5173/partner"

    var fakeWebhookSecret: String? = null

    /** Enables `fake-complete`. Only `application-local.yml` turns it on. */
    var fakeCompleteEnabled: Boolean = false
}
