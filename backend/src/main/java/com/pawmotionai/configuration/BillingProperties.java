package com.pawmotionai.configuration;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "app.billing")
@Validated
public class BillingProperties {

    @NotBlank
    private String currency = "BRL";

    @NotNull
    @DecimalMin(value = "0.000001", inclusive = false)
    private BigDecimal creditsPerCurrencyUnit = BigDecimal.ONE;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal minPurchaseAmount = new BigDecimal("10.00");

    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal maxPurchaseAmount = new BigDecimal("10000.00");

    @NotBlank
    private String provider = "fake";

    @NotNull
    private Duration pendingTtl = Duration.ofHours(24);

    @NotBlank
    private String partnerPortalBaseUrl = "http://localhost:5173/partner";

    @NotBlank
    private String fakeWebhookSecret = "fake-webhook-secret";

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getCreditsPerCurrencyUnit() { return creditsPerCurrencyUnit; }
    public void setCreditsPerCurrencyUnit(BigDecimal creditsPerCurrencyUnit) { this.creditsPerCurrencyUnit = creditsPerCurrencyUnit; }
    public BigDecimal getMinPurchaseAmount() { return minPurchaseAmount; }
    public void setMinPurchaseAmount(BigDecimal minPurchaseAmount) { this.minPurchaseAmount = minPurchaseAmount; }
    public BigDecimal getMaxPurchaseAmount() { return maxPurchaseAmount; }
    public void setMaxPurchaseAmount(BigDecimal maxPurchaseAmount) { this.maxPurchaseAmount = maxPurchaseAmount; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Duration getPendingTtl() { return pendingTtl; }
    public void setPendingTtl(Duration pendingTtl) { this.pendingTtl = pendingTtl; }
    public String getPartnerPortalBaseUrl() { return partnerPortalBaseUrl; }
    public void setPartnerPortalBaseUrl(String partnerPortalBaseUrl) { this.partnerPortalBaseUrl = partnerPortalBaseUrl; }
    public String getFakeWebhookSecret() { return fakeWebhookSecret; }
    public void setFakeWebhookSecret(String fakeWebhookSecret) { this.fakeWebhookSecret = fakeWebhookSecret; }
}
