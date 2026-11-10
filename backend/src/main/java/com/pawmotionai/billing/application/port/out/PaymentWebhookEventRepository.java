package com.pawmotionai.billing.application.port.out;

import java.time.Instant;

public interface PaymentWebhookEventRepository {

    boolean exists(String provider, String providerEventId);

    void save(String provider, String providerEventId, String payloadHash, Instant receivedAt);
}
