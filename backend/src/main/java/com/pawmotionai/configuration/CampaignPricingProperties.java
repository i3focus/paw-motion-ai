package com.pawmotionai.configuration;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "app.campaign")
@Validated
public class CampaignPricingProperties {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal priceAmount = new BigDecimal("50.00");

    @Min(1)
    private long creationCostCredits = 500;

    @Min(1)
    private int durationDays = 30;

    public BigDecimal getPriceAmount() { return priceAmount; }
    public void setPriceAmount(BigDecimal priceAmount) { this.priceAmount = priceAmount; }
    public long getCreationCostCredits() { return creationCostCredits; }
    public void setCreationCostCredits(long creationCostCredits) { this.creationCostCredits = creationCostCredits; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
}
