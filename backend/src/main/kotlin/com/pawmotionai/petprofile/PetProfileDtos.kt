package com.pawmotionai.petprofile

import com.pawmotionai.engagement.BadgeDto
import com.pawmotionai.engagement.PetEngagementDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProfilePetDto(
    val id: UUID,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: LocalDate?,
    val size: String,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val activityPreference: String?,
)

data class ProfileOwnerDto(
    val id: UUID,
    val displayName: String,
    val neighborhood: String?,
    val city: String?,
)

data class ProfileStatsDto(
    val followers: Long,
    val adventures: Long,
    val moments: Long,
)

data class WeeklyBarDto(
    val day: String,
    val minutes: Long,
)

data class PetProfileDto(
    val pet: ProfilePetDto,
    val owner: ProfileOwnerDto,
    val engagement: PetEngagementDto,
    val stats: ProfileStatsDto,
    val badgeShelf: List<BadgeDto>,
    val weeklyActivity: List<WeeklyBarDto>,
    val following: Boolean,
    val own: Boolean,
)

data class PetPostDto(
    val postId: UUID,
    val feedItemId: UUID,
    val caption: String?,
    val placeName: String?,
    val media: List<String>,
    val publishedAt: Instant,
    val reactionCount: Long,
    val commentCount: Long,
)

data class AdventureDto(
    val activityId: UUID,
    val feedItemId: UUID?,
    val type: String,
    val startedAt: Instant,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val notes: String?,
)

data class PetFriendDto(
    val petId: UUID,
    val name: String,
    val species: String,
    val breed: String?,
    val avatarUrl: String?,
    val ownerName: String,
    val mutualSince: Instant,
)
