package com.pawmotionai.billing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.billing.domain.model.PaymentTransaction;

public interface PaymentTransactionRepository {

    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findById(UUID id);

    Optional<PaymentTransaction> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey);

    Optional<PaymentTransaction> findByProviderAndProviderCheckoutId(String provider, String providerCheckoutId);

    List<PaymentTransaction> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    List<PaymentTransaction> findPendingCreatedBefore(Instant createdBefore);
}
