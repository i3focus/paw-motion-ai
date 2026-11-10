package com.pawmotionai.ledger.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryDirection direction;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private LedgerReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    private String description;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LedgerEntryJpaEntity() {
    }

    public LedgerEntryJpaEntity(UUID id, UUID accountId, LedgerEntryDirection direction, long amount, long balanceAfter,
                                LedgerReferenceType referenceType, UUID referenceId, String description,
                                String correlationId, Instant occurredAt) {
        this.id = id;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public LedgerEntryDirection getDirection() { return direction; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public LedgerReferenceType getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public String getDescription() { return description; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
