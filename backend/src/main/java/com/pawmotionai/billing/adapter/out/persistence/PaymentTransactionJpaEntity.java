package com.pawmotionai.billing.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.billing.domain.model.PaymentStatus;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "cpp_snapshot", nullable = false)
    private BigDecimal cppSnapshot;

    @Column(name = "expected_credits", nullable = false)
    private long expectedCredits;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    private String method;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_checkout_id")
    private String providerCheckoutId;

    @Column(name = "provider_checkout_url")
    private String providerCheckoutUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "customer_id", length = 160)
    private String customerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PaymentTransactionJpaEntity() {
    }

    public PaymentTransactionJpaEntity(UUID id, UUID partnerId, BigDecimal amount, String currency, BigDecimal cppSnapshot,
                                       long expectedCredits, UUID targetAccountId, String method, String provider,
                                       String idempotencyKey, Instant createdAt) {
        this(id, partnerId, amount, currency, cppSnapshot, expectedCredits, targetAccountId, method, provider,
            idempotencyKey, createdAt, null);
    }

    public PaymentTransactionJpaEntity(UUID id, UUID partnerId, BigDecimal amount, String currency, BigDecimal cppSnapshot,
                                       long expectedCredits, UUID targetAccountId, String method, String provider,
                                       String idempotencyKey, Instant createdAt, String customerId) {
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
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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
    public void setProviderCheckoutId(String providerCheckoutId) { this.providerCheckoutId = providerCheckoutId; }
    public String getProviderCheckoutUrl() { return providerCheckoutUrl; }
    public void setProviderCheckoutUrl(String providerCheckoutUrl) { this.providerCheckoutUrl = providerCheckoutUrl; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCustomerId() { return customerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
