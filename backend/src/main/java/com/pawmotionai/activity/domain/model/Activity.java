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
    private Double latitude;
    private Double longitude;

    private Activity(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt, long durationSeconds,
                     Double distanceMeters, String notes, Instant createdAt, Double latitude, Double longitude) {
        this.id = id;
        this.ownerId = ownerId;
        this.petId = petId;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.notes = notes;
        this.createdAt = createdAt;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Activity record(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                  long durationSeconds, Double distanceMeters, String notes, Instant createdAt,
                                  Double latitude, Double longitude) {
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
        validateCoordinate(latitude, -90, 90, "latitude");
        validateCoordinate(longitude, -180, 180, "longitude");
        return new Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt,
            latitude, longitude);
    }

    public static Activity record(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                  long durationSeconds, Double distanceMeters, String notes, Instant createdAt) {
        return record(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt, null, null);
    }

    public static Activity reconstitute(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                        long durationSeconds, Double distanceMeters, String notes, Instant createdAt,
                                        Double latitude, Double longitude) {
        return new Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt,
            latitude, longitude);
    }

    public static Activity reconstitute(UUID id, UUID ownerId, UUID petId, ActivityType type, Instant startedAt,
                                        long durationSeconds, Double distanceMeters, String notes, Instant createdAt) {
        return reconstitute(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt,
            null, null);
    }

    private static void validateCoordinate(Double value, double min, double max, String field) {
        if (value != null && (value < min || value > max)) {
            throw new InvalidActivityException(field + " must be between " + min + " and " + max);
        }
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
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
