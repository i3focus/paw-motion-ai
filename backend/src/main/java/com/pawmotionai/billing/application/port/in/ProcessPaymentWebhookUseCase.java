package com.pawmotionai.billing.application.port.in;

import com.pawmotionai.billing.application.port.out.WebhookRequest;

public interface ProcessPaymentWebhookUseCase {

    void process(WebhookRequest request);
}
