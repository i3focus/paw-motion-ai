package com.pawmotionai.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Owner {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private String displayName;
    private final String role;
    private final OwnerStatus status;
    private final Instant createdAt;
    private String bio;
    private String phone;
    private String avatarUrl;

    private Owner(UUID id, String email, String passwordHash, String displayName, String role, OwnerStatus status,
                  Instant createdAt, String bio, String phone, String avatarUrl) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.bio = bio;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }

    public static Owner register(UUID id, String email, String passwordHash, String displayName, Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, "OWNER", OwnerStatus.ACTIVE, createdAt,
            null, null, null);
    }

    public static Owner registerPartner(UUID id, String email, String passwordHash, String displayName,
                                        Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, "PARTNER", OwnerStatus.ACTIVE, createdAt,
            null, null, null);
    }

    public static Owner reconstitute(UUID id, String email, String passwordHash, String displayName, String role,
                                     OwnerStatus status, Instant createdAt, String bio, String phone, String avatarUrl) {
        return new Owner(id, email, passwordHash, displayName, role, status, createdAt, bio, phone, avatarUrl);
    }

    public boolean isActive() {
        return status == OwnerStatus.ACTIVE;
    }

    public boolean isPartner() {
        return "PARTNER".equals(role);
    }

    public void updateProfile(String displayName, String bio, String phone, String avatarUrl) {
        if (displayName != null) {
            String trimmedName = displayName.trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("Display name cannot be blank");
            }
            this.displayName = trimmedName;
        }
        if (bio != null) {
            this.bio = trimToNull(bio);
        }
        if (phone != null) {
            this.phone = trimToNull(phone);
        }
        if (avatarUrl != null) {
            this.avatarUrl = trimToNull(avatarUrl);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public OwnerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getBio() { return bio; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
}
