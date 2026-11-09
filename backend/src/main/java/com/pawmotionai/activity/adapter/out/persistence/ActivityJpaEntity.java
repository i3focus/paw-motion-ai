package com.pawmotionai.activity.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.activity.domain.model.ActivityType;

@Entity
@Table(name = "activities")
public class ActivityJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected ActivityJpaEntity() {
    }

    public ActivityJpaEntity(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt, long durationSeconds,
                             Double distanceMeters, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.petId = petId;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getPetId() { return petId; }
    public ActivityType getType() { return type; }
    public Instant getStartedAt() { return startedAt; }
    public long getDurationSeconds() { return durationSeconds; }
    public Double getDistanceMeters() { return distanceMeters; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
