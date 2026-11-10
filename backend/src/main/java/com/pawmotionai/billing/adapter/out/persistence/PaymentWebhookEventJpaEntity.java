package com.pawmotionai.billing.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_webhook_events")
@IdClass(PaymentWebhookEventId.class)
public class PaymentWebhookEventJpaEntity {

    @Id
    @Column(name = "provider", nullable = false)
    private String provider;

    @Id
    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected PaymentWebhookEventJpaEntity() {
    }

    public PaymentWebhookEventJpaEntity(String provider, String providerEventId, String payloadHash, Instant receivedAt) {
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.payloadHash = payloadHash;
        this.receivedAt = receivedAt;
    }

    public String getProvider() { return provider; }
    public String getProviderEventId() { return providerEventId; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getReceivedAt() { return receivedAt; }
}
