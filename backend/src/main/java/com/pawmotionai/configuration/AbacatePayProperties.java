package com.pawmotionai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.billing.abacatepay")
@Validated
public class AbacatePayProperties {

    private String apiKey = "";
    private String webhookSecret = "";
    private String creditProductExternalId = "pawmotion-credits-brl";
    private String baseUrl = "https://api.abacatepay.com/v2";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getCreditProductExternalId() { return creditProductExternalId; }
    public void setCreditProductExternalId(String creditProductExternalId) { this.creditProductExternalId = creditProductExternalId; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
