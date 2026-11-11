package com.pawmotionai.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.identity.domain.model.OwnerStatus;

@Entity
@Table(name = "owners")
public class OwnerJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnerStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column private String bio;
    @Column private String phone;
    @Column(name = "avatar_url") private String avatarUrl;

    @Version
    private Long version;

    protected OwnerJpaEntity() {
    }

    public OwnerJpaEntity(UUID id, String email, String passwordHash, String displayName, String role, OwnerStatus status,
                          Instant createdAt, Instant updatedAt, String bio, String phone, String avatarUrl) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.bio = bio;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public OwnerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getBio() { return bio; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public Long getVersion() { return version; }

    public void updateProfile(String displayName, String bio, String phone, String avatarUrl) {
        this.displayName = displayName;
        this.bio = bio;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }
}
