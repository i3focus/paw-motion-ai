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

import com.pawmotionai.ledger.domain.model.WalletStatus;

@Entity
@Table(name = "ledger_wallets")
public class LedgerWalletJpaEntity {

    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected LedgerWalletJpaEntity() {
    }

    public LedgerWalletJpaEntity(UUID id, UUID partnerId, WalletStatus status, String blockedReason,
                                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.status = status;
        this.blockedReason = blockedReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
