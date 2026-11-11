package com.pawmotionai.notification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class NotificationType {
    HEALTH_REMINDER,
    ACHIEVEMENT,
    CHALLENGE,
    SOCIAL,
    PLAYDATE,
    LOST_PET,
    BOOKING,
    SYSTEM,
}

enum class PushPlatform { IOS, ANDROID, WEB }

data class PushTokenRequest(
    @field:NotBlank @field:Size(max = 200) val token: String?,
    @field:NotNull val platform: PushPlatform?,
)

data class NotificationPreferencesDto(
    val healthReminders: Boolean = true,
    val social: Boolean = true,
    val playdates: Boolean = true,
    val lostPets: Boolean = true,
    val bookings: Boolean = true,
)

data class NotificationDto(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, Any?>,
    val read: Boolean,
    val createdAt: Instant,
)
