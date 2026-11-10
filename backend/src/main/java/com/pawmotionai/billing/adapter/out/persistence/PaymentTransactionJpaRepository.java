package com.pawmotionai.billing.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawmotionai.billing.domain.model.PaymentStatus;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionJpaEntity, UUID> {

    List<PaymentTransactionJpaEntity> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    Optional<PaymentTransactionJpaEntity> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey);

    Optional<PaymentTransactionJpaEntity> findByProviderAndProviderCheckoutId(String provider, String providerCheckoutId);

    List<PaymentTransactionJpaEntity> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdBefore);
}
