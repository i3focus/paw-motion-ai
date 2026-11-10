package com.pawmotionai.billing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException;

public class PaymentTransaction {

    private final UUID id;
    private final UUID partnerId;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal cppSnapshot;
    private final long expectedCredits;
    private final UUID targetAccountId;
    private final String method;
    private final String provider;
    private String providerCheckoutId;
    private String providerCheckoutUrl;
    private PaymentStatus status;
    private String failureReason;
    private final String idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentTransaction(UUID id, UUID partnerId, BigDecimal amount, String currency, BigDecimal cppSnapshot,
                              long expectedCredits, UUID targetAccountId, String method, String provider,
                              String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.amount = amount;
        this.currency = currency;
        this.cppSnapshot = cppSnapshot;
        this.expectedCredits = expectedCredits;
        this.targetAccountId = targetAccountId;
        this.method = method;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static PaymentTransaction reconstitute(UUID id, UUID partnerId, BigDecimal amount, String currency,
                                                  BigDecimal cppSnapshot, long expectedCredits, UUID targetAccountId,
                                                  String method, String provider, String providerCheckoutId,
                                                  String providerCheckoutUrl, PaymentStatus status, String failureReason,
                                                  String idempotencyKey, Instant createdAt, Instant updatedAt) {
        PaymentTransaction transaction = new PaymentTransaction(id, partnerId, amount, currency, cppSnapshot,
            expectedCredits, targetAccountId, method, provider, idempotencyKey, createdAt);
        transaction.providerCheckoutId = providerCheckoutId;
        transaction.providerCheckoutUrl = providerCheckoutUrl;
        transaction.status = status;
        transaction.failureReason = failureReason;
        transaction.updatedAt = updatedAt;
        return transaction;
    }

    public void attachCheckout(String providerCheckoutId, String providerCheckoutUrl, Instant updatedAt) {
        requirePendingForMutation();
        this.providerCheckoutId = providerCheckoutId;
        this.providerCheckoutUrl = providerCheckoutUrl;
        this.updatedAt = updatedAt;
    }

    public void confirm(Instant updatedAt) {
        if (!canConfirm()) {
            throw new InvalidPaymentStateException("Cannot confirm payment in status " + status);
        }
        this.status = PaymentStatus.CONFIRMED;
        this.failureReason = null;
        this.updatedAt = updatedAt;
    }

    public void fail(String reason, Instant updatedAt) {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Cannot fail payment in status " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = updatedAt;
    }

    public void expire(Instant updatedAt) {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Cannot expire payment in status " + status);
        }
        this.status = PaymentStatus.EXPIRED;
        this.updatedAt = updatedAt;
    }

    public void cancel(Instant updatedAt) {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Cannot cancel payment in status " + status);
        }
        this.status = PaymentStatus.CANCELED;
        this.updatedAt = updatedAt;
    }

    public void dispute(Instant updatedAt) {
        if (status != PaymentStatus.CONFIRMED) {
            throw new InvalidPaymentStateException("Cannot dispute payment in status " + status);
        }
        this.status = PaymentStatus.DISPUTED;
        this.updatedAt = updatedAt;
    }

    public void refund(Instant updatedAt) {
        if (status != PaymentStatus.CONFIRMED && status != PaymentStatus.DISPUTED) {
            throw new InvalidPaymentStateException("Cannot refund payment in status " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = updatedAt;
    }

    public boolean canConfirm() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.EXPIRED || status == PaymentStatus.DISPUTED;
    }

    public boolean isPending() { return status == PaymentStatus.PENDING; }
    public boolean isConfirmed() { return status == PaymentStatus.CONFIRMED; }

    private void requirePendingForMutation() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Cannot attach checkout in status " + status);
        }
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getCppSnapshot() { return cppSnapshot; }
    public long getExpectedCredits() { return expectedCredits; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public String getMethod() { return method; }
    public String getProvider() { return provider; }
    public String getProviderCheckoutId() { return providerCheckoutId; }
    public String getProviderCheckoutUrl() { return providerCheckoutUrl; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
