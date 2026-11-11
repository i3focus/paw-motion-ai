package com.pawmotionai.feed.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class FeedCardType { POST, ACTIVITY, ACHIEVEMENT, SPONSORED }

enum class ReactionType { PAW, LAUGH, HEART }

enum class AdEventType { IMPRESSION, CLICK }

enum class AdPlacement { FEED_CARD, MAP_PIN, DISCOVER_HIGHLIGHT, NEARBY_PROMO, AI_RECOMMENDATION }

data class FeedAuthorDto(
    val ownerId: UUID,
    val ownerName: String,
    val petId: UUID?,
    val petName: String?,
    val petAvatarUrl: String?,
    val petSpecies: String?,
    val neighborhood: String?,
)

data class MediaDto(val id: UUID, val url: String, val contentType: String)

data class PostContentDto(
    val postId: UUID,
    val caption: String?,
    val placeName: String?,
    val media: List<MediaDto>,
    val taggedPetIds: List<UUID>,
)

data class ActivityContentDto(
    val activityId: UUID,
    val type: String,
    val startedAt: Instant,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val notes: String?,
)

data class AchievementContentDto(val badgeCode: String, val name: String, val description: String, val emoji: String)

data class SponsoredContentDto(
    val campaignId: UUID,
    val partnerId: UUID,
    val partnerName: String,
    val verified: Boolean,
    val title: String,
    val description: String,
    val ctaLabel: String,
    val ctaType: String,
    val ctaUrl: String?,
    val serviceId: UUID?,
    val mediaUrl: String?,
    val label: String = "Sponsored",
)

data class ReactionSummaryDto(val counts: Map<ReactionType, Long>, val viewerReaction: ReactionType?)

data class FeedCardDto(
    val id: UUID,
    val type: FeedCardType,
    val publishedAt: Instant?,
    val author: FeedAuthorDto?,
    val post: PostContentDto? = null,
    val activity: ActivityContentDto? = null,
    val achievement: AchievementContentDto? = null,
    val sponsored: SponsoredContentDto? = null,
    val reactions: ReactionSummaryDto? = null,
    val commentCount: Long = 0,
)

data class CommentDto(
    val id: UUID,
    val feedItemId: UUID,
    val ownerId: UUID,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val body: String,
    val createdAt: Instant,
)

data class ReactRequest(@field:NotNull val type: ReactionType?)

data class CommentRequest(@field:NotBlank @field:Size(max = 500) val body: String?)

data class AdEventRequest(@field:NotNull val type: AdEventType?, @field:NotNull val placement: AdPlacement?)
