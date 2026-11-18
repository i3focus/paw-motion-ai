package com.pawmotionai.configuration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

@Component
class PricingValidationRunner(
    private val billingProperties: BillingProperties,
    private val campaignPricingProperties: CampaignPricingProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(PricingValidationRunner::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            Currency.getInstance(billingProperties.currency)
        } catch (exception: IllegalArgumentException) {
            log.warn("BILLING_CURRENCY '{}' is not a valid ISO-4217 currency; purchase conversions may fail.",
                billingProperties.currency)
        }
        val campaignCreditsFromBilling = campaignPricingProperties.priceAmount
            .multiply(billingProperties.creditsPerCurrencyUnit)
            .setScale(0, RoundingMode.DOWN)
        if (campaignCreditsFromBilling.compareTo(BigDecimal.valueOf(campaignPricingProperties.creationCostCredits)) != 0) {
            log.warn("CAMPAIGN_CREATION_COST_CREDITS ({}) does not match CAMPAIGN_PRICE_AMOUNT ({}) multiplied by CPP ({}); check operator intent.",
                campaignPricingProperties.creationCostCredits, campaignPricingProperties.priceAmount,
                billingProperties.creditsPerCurrencyUnit)
        }
    }
}
