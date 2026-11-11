package com.pawmotionai.engagement

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ChallengeMetric { ACTIVITY_COUNT, DISTANCE_METERS, DURATION_SECONDS }

data class BadgeDto(
    val code: String,
    val name: String,
    val description: String,
    val emoji: String,
    val awarded: Boolean,
    val awardedAt: Instant?,
)

data class PetEngagementDto(
    val petId: UUID,
    val xp: Long,
    val level: Int,
    val levelTitle: String,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long?,
    val streakDays: Int,
    val badgeCount: Long,
)

data class ChallengeParticipantDto(
    val petId: UUID,
    val petName: String,
    val progress: Long,
    val completed: Boolean,
)

data class ChallengeDto(
    val id: UUID,
    val title: String,
    val description: String,
    val metric: ChallengeMetric,
    val targetValue: Long,
    val xpReward: Int,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val author: String,
    val joined: List<ChallengeParticipantDto>,
)

data class LeaderboardEntryDto(
    val rank: Int,
    val petId: UUID,
    val petName: String,
    val petAvatarUrl: String?,
    val ownerName: String,
    val neighborhood: String?,
    val level: Int,
    val levelTitle: String,
    val weeklyXp: Long,
    val own: Boolean,
)

data class LeaderboardDto(val scope: String, val entries: List<LeaderboardEntryDto>)

data class JoinChallengeRequest(@field:NotNull val petId: UUID?)

data class CreateChallengeRequest(
    @field:NotBlank @field:Size(max = 120) val title: String?,
    @field:NotBlank @field:Size(max = 300) val description: String?,
    @field:NotNull val metric: ChallengeMetric?,
    @field:NotNull @field:Min(1) @field:Max(1000000) val targetValue: Long?,
    @field:NotNull @field:Min(1) @field:Max(10000) val xpReward: Int?,
    @field:NotNull val startsOn: LocalDate?,
    @field:NotNull val endsOn: LocalDate?,
)
