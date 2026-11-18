package com.pawmotionai.configuration

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal

@Component
@ConfigurationProperties(prefix = "app.campaign")
@Validated
class CampaignPricingProperties {

    @field:NotNull
    @field:DecimalMin(value = "0.01", inclusive = false)
    var priceAmount: BigDecimal = BigDecimal("50.00")

    @field:Min(1)
    var creationCostCredits: Long = 500

    @field:Min(1)
    var durationDays: Int = 30
}
