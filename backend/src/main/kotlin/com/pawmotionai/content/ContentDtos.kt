package com.pawmotionai.content

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreatePostRequest(
    @field:NotNull val petId: UUID?,
    @field:Size(max = 200) val caption: String?,
    @field:Size(max = 160) val placeName: String?,
    @field:DecimalMin("-90") @field:DecimalMax("90") val latitude: Double?,
    @field:DecimalMin("-180") @field:DecimalMax("180") val longitude: Double?,
    @field:NotNull @field:Size(min = 1, max = 10) val mediaIds: List<UUID>?,
    @field:Size(max = 10) val taggedPetIds: List<UUID>? = emptyList(),
)

data class PostCreatedDto(val postId: UUID, val feedItemId: UUID)

data class CreateStoryRequest(
    @field:NotNull val petId: UUID?,
    @field:NotNull val mediaId: UUID?,
    @field:Size(max = 120) val caption: String?,
)

data class StoryDto(
    val id: UUID,
    val mediaUrl: String,
    val contentType: String,
    val caption: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val viewed: Boolean,
)

data class StoryGroupDto(
    val petId: UUID,
    val petName: String,
    val petAvatarUrl: String?,
    val ownerId: UUID,
    val own: Boolean,
    val allViewed: Boolean,
    val stories: List<StoryDto>,
)

data class FollowStateDto(val petId: UUID, val following: Boolean, val followers: Long)
