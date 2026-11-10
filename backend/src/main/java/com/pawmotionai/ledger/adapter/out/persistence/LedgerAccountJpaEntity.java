package com.pawmotionai.ledger.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountCode code;

    @Column(name = "debit_priority", nullable = false)
    private int debitPriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccountStatus status;

    @Column(nullable = false)
    private long balance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected LedgerAccountJpaEntity() {
    }

    public LedgerAccountJpaEntity(UUID id, UUID walletId, LedgerAccountCode code, int debitPriority,
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

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public LedgerAccountCode getCode() { return code; }
    public int getDebitPriority() { return debitPriority; }
    public LedgerAccountStatus getStatus() { return status; }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
    public void setStatus(LedgerAccountStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
