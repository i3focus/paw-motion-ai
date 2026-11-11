package com.pawmotionai.ads

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class CampaignCtaType { LINK, BOOKING }

enum class AdFormat { FEED_CARD, MAP_PIN, DISCOVER_HIGHLIGHT, NEARBY_PROMO, AI_RECOMMENDATION }

enum class PartnerCategory { VET, GROOMING, HOTEL, CAFE, PET_SHOP, TRAINING, WALKER, OTHER }

data class CampaignV2Dto(
    val id: UUID,
    val status: String,
    val endReason: String?,
    val title: String,
    val description: String,
    val targetAudience: String,
    val ctaLabel: String,
    val ctaType: String,
    val ctaUrl: String?,
    val serviceId: UUID?,
    val mediaUrl: String?,
    val budgetCredits: Long,
    val creditsCharged: Long,
    val creditsRefunded: Long,
    val startsAt: Instant,
    val endsAt: Instant,
    val targetLocation: TargetLocationDto?,
    val targetSpecies: List<String>,
    val targetBreeds: List<String>,
    val targetInterests: List<String>,
    val formats: List<String>,
    val reviewedAt: Instant?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TargetLocationDto(val latitude: Double, val longitude: Double, val radiusMeters: Int)

data class UpdateCampaignRequest(
    @field:Size(max = 160) val title: String?,
    @field:Size(max = 2000) val description: String?,
    @field:Size(max = 160) val targetAudience: String?,
    @field:Size(max = 160) val ctaLabel: String?,
    val ctaType: CampaignCtaType?,
    @field:Size(max = 500) val ctaUrl: String?,
    val serviceId: UUID?,
    val mediaAssetId: UUID?,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val targetLatitude: Double?,
    val targetLongitude: Double?,
    @field:Min(500) @field:Max(100000) val targetRadiusMeters: Int?,
    @field:Size(max = 10) val targetSpecies: List<@Size(max = 32) String>?,
    @field:Size(max = 20) val targetBreeds: List<@Size(max = 80) String>?,
    @field:Size(max = 20) val targetInterests: List<@Size(max = 48) String>?,
    @field:Size(min = 1, max = 5) val formats: List<AdFormat>?,
)

data class CampaignMetricsDto(
    val campaignId: UUID,
    val from: Instant,
    val to: Instant,
    val impressions: Long,
    val clicks: Long,
    val bookings: Long,
    val clickThroughRate: Double,
    val bookingRate: Double,
    val creditsSpent: Long,
    val costPerClick: Double?,
    val costPerBooking: Double?,
    val byPlacement: Map<String, Map<String, Long>>,
)

data class PartnerProfileDto(
    val partnerId: UUID,
    val businessName: String,
    val category: PartnerCategory,
    val description: String?,
    val website: String?,
    val phone: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val openingHours: String?,
    val coverUrl: String?,
    val verified: Boolean,
    val verifiedAt: Instant?,
    val placeId: UUID?,
)

data class UpdatePartnerProfileRequest(
    @field:Size(max = 160) val businessName: String?,
    val category: PartnerCategory?,
    @field:Size(max = 1000) val description: String?,
    @field:Size(max = 300) val website: String?,
    @field:Size(max = 40) val phone: String?,
    @field:Size(max = 300) val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    @field:Size(max = 300) val openingHours: String?,
    @field:Size(max = 500) val coverUrl: String?,
)

data class AdminCampaignDto(
    val id: UUID,
    val partnerId: UUID,
    val partnerName: String,
    val partnerVerified: Boolean,
    val status: String,
    val title: String,
    val description: String,
    val ctaLabel: String,
    val ctaType: String,
    val formats: List<String>,
    val startsAt: Instant,
    val endsAt: Instant,
    val submittedAt: Instant,
    val reviewedAt: Instant?,
    val rejectionReason: String?,
)

data class RejectCampaignRequest(@field:Size(max = 500) val reason: String?)
