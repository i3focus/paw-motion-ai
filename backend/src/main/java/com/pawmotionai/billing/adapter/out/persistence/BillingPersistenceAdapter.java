package com.pawmotionai.billing.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.application.port.out.PaymentWebhookEventRepository;
import com.pawmotionai.billing.domain.model.PaymentStatus;
import com.pawmotionai.billing.domain.model.PaymentTransaction;

@Repository
public class BillingPersistenceAdapter implements PaymentTransactionRepository, PaymentWebhookEventRepository {

    private final PaymentTransactionJpaRepository transactionRepository;
    private final PaymentWebhookEventJpaRepository webhookEventRepository;

    public BillingPersistenceAdapter(PaymentTransactionJpaRepository transactionRepository,
                                     PaymentWebhookEventJpaRepository webhookEventRepository) {
        this.transactionRepository = transactionRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        PaymentTransactionJpaEntity entity = transactionRepository.findById(transaction.getId())
            .orElseGet(() -> new PaymentTransactionJpaEntity(transaction.getId(), transaction.getPartnerId(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getCppSnapshot(),
                transaction.getExpectedCredits(), transaction.getTargetAccountId(), transaction.getMethod(),
                transaction.getProvider(), transaction.getIdempotencyKey(), transaction.getCreatedAt()));
        entity.setProviderCheckoutId(transaction.getProviderCheckoutId());
        entity.setProviderCheckoutUrl(transaction.getProviderCheckoutUrl());
        entity.setStatus(transaction.getStatus());
        entity.setFailureReason(transaction.getFailureReason());
        entity.setUpdatedAt(transaction.getUpdatedAt());
        return toDomain(transactionRepository.save(entity));
    }

    @Override
    public Optional<PaymentTransaction> findById(UUID id) {
        return transactionRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey) {
        return transactionRepository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findByProviderAndProviderCheckoutId(String provider, String providerCheckoutId) {
        return transactionRepository.findByProviderAndProviderCheckoutId(provider, providerCheckoutId).map(this::toDomain);
    }

    @Override
    public List<PaymentTransaction> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId) {
        return transactionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentTransaction> findPendingCreatedBefore(Instant createdBefore) {
        return transactionRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, createdBefore)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean exists(String provider, String providerEventId) {
        return webhookEventRepository.existsByProviderAndProviderEventId(provider, providerEventId);
    }

    @Override
    public void save(String provider, String providerEventId, String payloadHash, Instant receivedAt) {
        webhookEventRepository.save(new PaymentWebhookEventJpaEntity(provider, providerEventId, payloadHash, receivedAt));
    }

    private PaymentTransaction toDomain(PaymentTransactionJpaEntity entity) {
        return PaymentTransaction.reconstitute(entity.getId(), entity.getPartnerId(), entity.getAmount(), entity.getCurrency(),
            entity.getCppSnapshot(), entity.getExpectedCredits(), entity.getTargetAccountId(), entity.getMethod(),
            entity.getProvider(), entity.getProviderCheckoutId(), entity.getProviderCheckoutUrl(), entity.getStatus(),
            entity.getFailureReason(), entity.getIdempotencyKey(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
