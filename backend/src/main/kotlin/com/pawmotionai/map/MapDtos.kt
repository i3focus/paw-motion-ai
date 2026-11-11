package com.pawmotionai.map

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class PlaceCategory { PARK, VET, CAFE, HOTEL, GROOMING, PET_SHOP, TRAINING, OTHER }
enum class PlaydateMode { NOW, SCHEDULED }
enum class InviteResponse { ACCEPTED, DECLINED }

data class PlaceDto(
    val id: UUID,
    val name: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double?,
    val address: String?,
    val description: String?,
    val rating: Double?,
    val ratingCount: Int,
    val partnerId: UUID?,
    val verifiedPartner: Boolean,
    val checkInsToday: Long,
)

data class CheckInRequest(@field:NotNull val petId: UUID?)
data class CheckInDto(val id: UUID, val placeId: UUID, val petId: UUID, val createdAt: Instant)

data class CreateLostPetAlertRequest(
    @field:NotNull val petId: UUID?,
    @field:NotBlank @field:Size(max = 500) val description: String?,
    @field:Size(max = 40) val contactPhone: String?,
    val photoMediaId: UUID?,
    @field:NotNull @field:DecimalMin("-90") @field:DecimalMax("90") val latitude: Double?,
    @field:NotNull @field:DecimalMin("-180") @field:DecimalMax("180") val longitude: Double?,
    @field:NotNull val lastSeenAt: Instant?,
    @field:Min(100) @field:Max(20000) val radiusMeters: Int? = 3000,
)

data class LostPetAlertDto(
    val id: UUID,
    val petId: UUID,
    val petName: String,
    val petSpecies: String,
    val ownerId: UUID,
    val description: String,
    val contactPhone: String?,
    val photoUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val lastSeenAt: Instant,
    val radiusMeters: Int,
    val status: String,
    val distanceMeters: Double?,
    val sightings: Long,
)

data class SightingRequest(
    @field:NotNull @field:DecimalMin("-90") @field:DecimalMax("90") val latitude: Double?,
    @field:NotNull @field:DecimalMin("-180") @field:DecimalMax("180") val longitude: Double?,
    @field:NotNull val seenAt: Instant?,
    @field:Size(max = 300) val note: String?,
)

data class ResolveAlertRequest(@field:NotNull val found: Boolean?)

data class CreatePlaydateRequest(
    @field:NotNull val petId: UUID?,
    @field:NotNull val mode: PlaydateMode?,
    val startsAt: Instant?,
    val placeId: UUID?,
    @field:DecimalMin("-90") @field:DecimalMax("90") val latitude: Double?,
    @field:DecimalMin("-180") @field:DecimalMax("180") val longitude: Double?,
    @field:Size(max = 160) val locationName: String?,
    @field:Size(max = 300) val message: String?,
    @field:Size(max = 10) val inviteePetIds: List<UUID>? = emptyList(),
)

data class PlaydateInviteDto(val petId: UUID, val petName: String, val status: String)

data class PlaydateDto(
    val id: UUID,
    val organizerOwnerId: UUID,
    val organizerPetId: UUID,
    val organizerPetName: String,
    val mode: PlaydateMode,
    val startsAt: Instant,
    val placeId: UUID?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val message: String?,
    val status: String,
    val invites: List<PlaydateInviteDto>,
)

data class RespondInviteRequest(@field:NotNull val petId: UUID?, @field:NotNull val response: InviteResponse?)

data class NearbyPetDto(val petId: UUID, val petName: String, val species: String, val breed: String?, val avatarUrl: String?, val ownerName: String, val distanceMeters: Double)
