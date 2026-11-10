package com.pawmotionai.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventJpaRepository extends JpaRepository<PaymentWebhookEventJpaEntity, PaymentWebhookEventId> {

    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
