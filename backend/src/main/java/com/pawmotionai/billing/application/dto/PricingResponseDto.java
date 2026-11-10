package com.pawmotionai.billing.application.dto;

import java.math.BigDecimal;

public record PricingResponseDto(String currency, BigDecimal creditsPerCurrencyUnit, BigDecimal minPurchaseAmount,
                                 BigDecimal maxPurchaseAmount, BigDecimal campaignPriceAmount,
                                 long campaignCreationCostCredits, int campaignDurationDays, String provider) {
}
