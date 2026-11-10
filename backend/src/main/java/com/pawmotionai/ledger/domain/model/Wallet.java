package com.pawmotionai.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Wallet {

    private final UUID id;
    private final UUID partnerId;
    private WalletStatus status;
    private String blockedReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public Wallet(UUID id, UUID partnerId, WalletStatus status, String blockedReason,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.status = status;
        this.blockedReason = blockedReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void block(String reason, Instant updatedAt) {
        this.status = WalletStatus.BLOCKED;
        this.blockedReason = reason;
        this.updatedAt = updatedAt;
    }

    public void unblock(Instant updatedAt) {
        this.status = WalletStatus.ACTIVE;
        this.blockedReason = null;
        this.updatedAt = updatedAt;
    }

    public boolean isBlocked() {
        return status == WalletStatus.BLOCKED;
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public WalletStatus getStatus() { return status; }
    public String getBlockedReason() { return blockedReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
