package com.pawmotionai.booking

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class BookingStatus { REQUESTED, CONFIRMED, CANCELLED, COMPLETED }
enum class BookingSource { APP, AI, AD }

data class PartnerServiceDto(
    val id: UUID,
    val partnerId: UUID,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val priceCents: Long,
    val currency: String,
    val active: Boolean,
)

data class PartnerProfileDto(
    val partnerId: UUID,
    val businessName: String,
    val category: String,
    val description: String?,
    val website: String?,
    val phone: String?,
    val address: String?,
    val openingHours: String?,
    val coverUrl: String?,
    val verified: Boolean,
    val placeId: UUID?,
    val services: List<PartnerServiceDto>,
)

data class PartnerServiceRequest(
    @field:NotBlank @field:Size(max = 120) val name: String?,
    @field:Size(max = 500) val description: String?,
    @field:NotNull @field:Min(5) @field:Max(1440) val durationMinutes: Int?,
    @field:NotNull @field:Min(0) val priceCents: Long?,
    val active: Boolean? = true,
)

data class CreateBookingRequest(
    @field:NotNull val serviceId: UUID?,
    @field:NotNull val petId: UUID?,
    @field:NotNull val startsAt: Instant?,
    @field:Size(max = 500) val notes: String?,
    val campaignId: UUID?,
)

data class BookingDto(
    val id: UUID,
    val ownerId: UUID,
    val ownerName: String,
    val petId: UUID,
    val petName: String,
    val partnerId: UUID,
    val partnerName: String,
    val serviceId: UUID,
    val serviceName: String,
    val priceCents: Long,
    val currency: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: BookingStatus,
    val source: BookingSource,
    val campaignId: UUID?,
    val notes: String?,
)
