package com.pawmotionai.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

public class LedgerEntry {

    private final UUID id;
    private final UUID accountId;
    private final LedgerEntryDirection direction;
    private final long amount;
    private final long balanceAfter;
    private final LedgerReferenceType referenceType;
    private final UUID referenceId;
    private final String description;
    private final String correlationId;
    private final Instant occurredAt;

    public LedgerEntry(UUID id, UUID accountId, LedgerEntryDirection direction, long amount, long balanceAfter,
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
