package com.pawmotionai.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException;

public class LedgerAccount {

    private final UUID id;
    private final UUID walletId;
    private final LedgerAccountCode code;
    private final int debitPriority;
    private LedgerAccountStatus status;
    private long balance;
    private final Instant createdAt;
    private Instant updatedAt;

    public LedgerAccount(UUID id, UUID walletId, LedgerAccountCode code, int debitPriority,
                         LedgerAccountStatus status, long balance, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.walletId = walletId;
        this.code = code;
        this.debitPriority = debitPriority;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void credit(long amount, Instant occurredAt) {
        requirePositive(amount);
        requireOpen();
        this.balance += amount;
        this.updatedAt = occurredAt;
    }

    public void debit(long amount, Instant occurredAt) {
        requirePositive(amount);
        requireOpen();
        if (amount > balance) {
            throw new InsufficientCreditsException(amount, balance);
        }
        this.balance -= amount;
        this.updatedAt = occurredAt;
    }

    public void close(Instant occurredAt) {
        this.status = LedgerAccountStatus.CLOSED;
        this.updatedAt = occurredAt;
    }

    private void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Ledger amount must be positive");
        }
    }

    private void requireOpen() {
        if (status != LedgerAccountStatus.OPEN) {
            throw new IllegalStateException("Ledger account " + code + " is closed");
        }
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public LedgerAccountCode getCode() { return code; }
    public int getDebitPriority() { return debitPriority; }
    public LedgerAccountStatus getStatus() { return status; }
    public long getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
