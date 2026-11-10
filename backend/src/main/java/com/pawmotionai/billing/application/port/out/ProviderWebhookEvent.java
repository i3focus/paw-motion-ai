package com.pawmotionai.billing.application.port.out;

public record ProviderWebhookEvent(String providerEventId, String eventType, String externalId, String checkoutId,
                                   boolean devMode) {
}
