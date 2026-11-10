package com.pawmotionai.configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PricingValidationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PricingValidationRunner.class);

    private final BillingProperties billingProperties;
    private final CampaignPricingProperties campaignPricingProperties;

    public PricingValidationRunner(BillingProperties billingProperties,
                                   CampaignPricingProperties campaignPricingProperties) {
        this.billingProperties = billingProperties;
        this.campaignPricingProperties = campaignPricingProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Currency.getInstance(billingProperties.getCurrency());
        } catch (IllegalArgumentException exception) {
            log.warn("BILLING_CURRENCY '{}' is not a valid ISO-4217 currency; purchase conversions may fail.",
                billingProperties.getCurrency());
        }
        BigDecimal campaignCreditsFromBilling = campaignPricingProperties.getPriceAmount()
            .multiply(billingProperties.getCreditsPerCurrencyUnit())
            .setScale(0, RoundingMode.DOWN);
        if (campaignCreditsFromBilling.compareTo(BigDecimal.valueOf(campaignPricingProperties.getCreationCostCredits())) != 0) {
            log.warn("CAMPAIGN_CREATION_COST_CREDITS ({}) does not match CAMPAIGN_PRICE_AMOUNT ({}) multiplied by CPP ({}); check operator intent.",
                campaignPricingProperties.getCreationCostCredits(), campaignPricingProperties.getPriceAmount(),
                billingProperties.getCreditsPerCurrencyUnit());
        }
    }
}
