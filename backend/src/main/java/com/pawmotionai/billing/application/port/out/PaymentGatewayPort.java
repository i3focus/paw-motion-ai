package com.pawmotionai.billing.application.port.out;

import com.pawmotionai.billing.domain.exception.WebhookVerificationException;

public interface PaymentGatewayPort {

    String provider();

    CheckoutSession createCheckout(CreateCheckoutRequest request);

    CheckoutDetails fetchCheckoutByExternalId(String externalId);

    void deleteCheckout(String providerCheckoutId);

    ProviderWebhookEvent verifyWebhook(WebhookRequest request) throws WebhookVerificationException;

    boolean expectedDevMode();
}
