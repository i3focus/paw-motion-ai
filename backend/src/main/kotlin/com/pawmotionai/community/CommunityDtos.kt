package com.pawmotionai.community

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class CommunityKind { BREED, NEIGHBORHOOD, CAUSE }

enum class RsvpStatus { GOING, INTERESTED }

data class CommunityDto(
    val id: UUID,
    val slug: String,
    val name: String,
    val kind: CommunityKind,
    val description: String?,
    val coverUrl: String?,
    val memberCount: Long,
    val joined: Boolean,
    val role: String?,
    val upcomingEvents: Long,
)

data class CommunityEventDto(
    val id: UUID,
    val communityId: UUID,
    val communityName: String?,
    val title: String,
    val description: String?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val startsAt: Instant,
    val endsAt: Instant?,
    val capacity: Int?,
    val goingCount: Long,
    val interestedCount: Long,
    val viewerRsvp: RsvpStatus?,
)

data class CreateCommunityRequest(
    @field:NotBlank @field:Size(max = 120) val name: String?,
    @field:NotNull val kind: CommunityKind?,
    @field:Size(max = 500) val description: String?,
    @field:Size(max = 500) val coverUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class CreateEventRequest(
    @field:NotBlank @field:Size(max = 120) val title: String?,
    @field:Size(max = 500) val description: String?,
    @field:Size(max = 160) val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    @field:NotNull val startsAt: Instant?,
    val endsAt: Instant?,
    @field:Min(1) @field:Max(10000) val capacity: Int?,
)

data class RsvpRequest(@field:NotNull val status: RsvpStatus?)

data class DiscoverDto(
    val sponsored: List<DiscoverPartnerDto>,
    val communities: List<CommunityDto>,
    val events: List<CommunityEventDto>,
)

data class DiscoverPartnerDto(
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
