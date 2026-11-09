package com.pawmotionai.activity.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.activity.domain.exception.InvalidActivityException;

public class Activity {

    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private final UUID id;
    private final UUID ownerId;
    private final UUID petId;
    private final ActivityType type;
    private final Instant startedAt;
    private final long durationSeconds;
    private final Double distanceMeters;
    private final String notes;
    private final Instant createdAt;

    private Activity(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt, long durationSeconds,
                     Double distanceMeters, String notes, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.petId = petId;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static Activity record(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                  long durationSeconds, Double distanceMeters, String notes, Instant createdAt) {
        if (durationSeconds <= 0) {
            throw new InvalidActivityException("durationSeconds must be greater than zero");
        }
        if (distanceMeters != null && distanceMeters < 0) {
            throw new InvalidActivityException("distanceMeters cannot be negative");
        }
        if (startedAt == null) {
            throw new InvalidActivityException("startedAt is required");
        }
        if (startedAt.isAfter(Instant.now().plus(FUTURE_TOLERANCE))) {
            throw new InvalidActivityException("startedAt cannot be in the future");
        }
        return new Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt);
    }

    public static Activity reconstitute(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                        long durationSeconds, Double distanceMeters, String notes, Instant createdAt) {
        return new Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt);
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
}
