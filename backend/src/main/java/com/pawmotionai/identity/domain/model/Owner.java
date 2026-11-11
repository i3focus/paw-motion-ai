package com.pawmotionai.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Owner {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private String displayName;
    private final String role;
    private final Instant createdAt;
    private OwnerStatus status;
    private String bio;
    private String phone;
    private String avatarUrl;
    private String businessName;
    private String website;
    private String partnerDescription;
    private Double latitude;
    private Double longitude;

    private Owner(UUID id, String email, String passwordHash, String displayName, String role, OwnerStatus status,
                  Instant createdAt, String bio, String phone, String avatarUrl, String businessName, String website,
                  String partnerDescription, Double latitude, Double longitude) {
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
        this.businessName = businessName;
        this.website = website;
        this.partnerDescription = partnerDescription;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Owner register(UUID id, String email, String passwordHash, String displayName, Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, "OWNER", OwnerStatus.ACTIVE, createdAt,
            null, null, null, null, null, null, null, null);
    }

    public static Owner registerPartner(UUID id, String email, String passwordHash, String displayName,
                                        String businessName, String website, String partnerDescription, Instant createdAt) {
        return new Owner(id, email, passwordHash, displayName, "PARTNER", OwnerStatus.ACTIVE, createdAt,
            null, null, null, businessName, website, partnerDescription, null, null);
    }

    public static Owner reconstitute(UUID id, String email, String passwordHash, String displayName, String role,
                                     OwnerStatus status, Instant createdAt, String bio, String phone, String avatarUrl,
                                     String businessName, String website, String partnerDescription,
                                     Double latitude, Double longitude) {
        return new Owner(id, email, passwordHash, displayName, role, status, createdAt, bio, phone, avatarUrl,
            businessName, website, partnerDescription, latitude, longitude);
    }

    public boolean isActive() {
        return status == OwnerStatus.ACTIVE;
    }

    public boolean isPartner() {
        return "PARTNER".equals(role);
    }

    public void updateProfile(String displayName, String bio, String phone, String avatarUrl,
                              String businessName, String website, String partnerDescription) {
        updateProfile(displayName, bio, phone, avatarUrl, businessName, website, partnerDescription, null, null);
    }

    public void updateProfile(String displayName, String bio, String phone, String avatarUrl,
                              String businessName, String website, String partnerDescription,
                              Double latitude, Double longitude) {
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
        if (businessName != null) {
            this.businessName = trimToNull(businessName);
        }
        if (website != null) {
            this.website = trimToNull(website);
        }
        if (partnerDescription != null) {
            this.partnerDescription = trimToNull(partnerDescription);
        }
        validateCoordinate(latitude, -90, 90, "latitude");
        validateCoordinate(longitude, -180, 180, "longitude");
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
    }

    private static void validateCoordinate(Double value, double min, double max, String field) {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
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
    public String getBusinessName() { return businessName; }
    public String getWebsite() { return website; }
    public String getPartnerDescription() { return partnerDescription; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
