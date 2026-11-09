package com.pawmotionai.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Owner {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final Instant createdAt;
    private OwnerStatus status;

    private Owner(UUID id, String email, String passwordHash, String displayName, String role, OwnerStatus status, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Owner register(UUID id, String email, String passwordHash, String displayName, Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, "OWNER", OwnerStatus.ACTIVE, createdAt);
    }

    public static Owner reconstitute(UUID id, String email, String passwordHash, String displayName, String role, OwnerStatus status, Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, role, status, createdAt);
    }

    public boolean isActive() {
        return status == OwnerStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public OwnerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
