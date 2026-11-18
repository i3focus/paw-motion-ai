package com.pawmotionai.activity.application.dto

import com.pawmotionai.activity.domain.model.Activity
import java.time.Instant
import java.util.UUID

data class RecordActivityCommand(
    val ownerId: UUID,
    val petId: UUID,
    val type: String,
    val startedAt: Instant?,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val notes: String?,
    val latitude: Double?,
    val longitude: Double?,
    val shareToPawCity: Boolean?,
)

data class ActivityResponseDto(
    val id: UUID,
    val ownerId: UUID,
    val petId: UUID,
    val type: String,
    val startedAt: Instant,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val notes: String?,
    val createdAt: Instant,
    val latitude: Double?,
    val longitude: Double?,
) {

    companion object {
        fun from(activity: Activity): ActivityResponseDto =
            ActivityResponseDto(
                activity.id, activity.ownerId, activity.petId, activity.type.name.lowercase(),
                activity.startedAt, activity.durationSeconds, activity.distanceMeters, activity.notes,
                activity.createdAt, activity.latitude, activity.longitude)
    }
}

data class DashboardSummaryDto(
    val ownerId: UUID,
    val petCount: Int,
    val activityCountLast7Days: Int,
    val totalDistanceMetersLast7Days: Double,
    val totalDurationSecondsLast7Days: Long,
    val currentStreakDays: Int,
    val recentActivities: List<ActivityResponseDto>,
)
