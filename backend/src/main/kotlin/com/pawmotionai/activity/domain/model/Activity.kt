package com.pawmotionai.activity.domain.model

import com.pawmotionai.activity.domain.exception.ActivityOutOfRangeException
import com.pawmotionai.activity.domain.exception.InvalidActivityException
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class ActivityType {
    WALK,
    RUN,
    HIKE,
    BIKE_RIDE,
    SWIM,
    PLAY
}

class Activity private constructor(
    val id: UUID,
    val ownerId: UUID,
    val petId: UUID,
    val type: ActivityType,
    val startedAt: Instant,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val notes: String?,
    val createdAt: Instant,
    val latitude: Double?,
    val longitude: Double?,
    val shareToPawCity: Boolean,
) {

    companion object {
        private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(5)
        const val MAX_DURATION_SECONDS: Long = 86_400
        const val MAX_DISTANCE_METERS: Double = 500_000.0
        val DEFAULT_MAX_BACKDATE: Duration = Duration.ofDays(14)

        fun record(id: UUID, ownerId: UUID, petId: UUID, type: ActivityType, startedAt: Instant?,
                   durationSeconds: Long, distanceMeters: Double?, notes: String?, createdAt: Instant,
                   latitude: Double? = null, longitude: Double? = null, shareToPawCity: Boolean = false,
                   maxBackdate: Duration = DEFAULT_MAX_BACKDATE, now: Instant = Instant.now()): Activity {
            if (durationSeconds <= 0) {
                throw InvalidActivityException("durationSeconds must be greater than zero")
            }
            if (durationSeconds > MAX_DURATION_SECONDS) {
                throw ActivityOutOfRangeException("durationSeconds cannot exceed $MAX_DURATION_SECONDS")
            }
            if (distanceMeters != null && !distanceMeters.isFinite()) {
                throw ActivityOutOfRangeException("distanceMeters must be a finite number")
            }
            if (distanceMeters != null && distanceMeters > MAX_DISTANCE_METERS) {
                throw ActivityOutOfRangeException("distanceMeters cannot exceed ${MAX_DISTANCE_METERS.toLong()}")
            }
            if (distanceMeters != null && distanceMeters < 0) {
                throw InvalidActivityException("distanceMeters cannot be negative")
            }
            if (startedAt == null) {
                throw InvalidActivityException("startedAt is required")
            }
            if (startedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
                throw InvalidActivityException("startedAt cannot be in the future")
            }
            if (startedAt.isBefore(now.minus(maxBackdate))) {
                throw ActivityOutOfRangeException("startedAt cannot be older than $maxBackdate")
            }
            validateCoordinate(latitude, -90.0, 90.0, "latitude")
            validateCoordinate(longitude, -180.0, 180.0, "longitude")
            return Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt,
                latitude, longitude, shareToPawCity)
        }

        fun reconstitute(id: UUID, ownerId: UUID, petId: UUID, type: ActivityType, startedAt: Instant,
                         durationSeconds: Long, distanceMeters: Double?, notes: String?, createdAt: Instant,
                         latitude: Double? = null, longitude: Double? = null, shareToPawCity: Boolean = false): Activity =
            Activity(id, ownerId, petId, type, startedAt, durationSeconds, distanceMeters, notes, createdAt,
                latitude, longitude, shareToPawCity)

        private fun validateCoordinate(value: Double?, min: Double, max: Double, field: String) {
            if (value != null && (value < min || value > max)) {
                throw InvalidActivityException("$field must be between $min and $max")
            }
        }
    }
}
