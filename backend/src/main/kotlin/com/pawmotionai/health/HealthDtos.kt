package com.pawmotionai.health

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class HealthRecordType { VACCINE, DEWORMING, VET_VISIT, MEDICATION }
enum class HealthStatus { DONE, UPCOMING, DUE_SOON, OVERDUE }

data class HealthRecordRequest(
    @field:NotNull val type: HealthRecordType?,
    @field:NotBlank @field:Size(max = 120) val name: String?,
    val administeredOn: LocalDate?,
    val dueOn: LocalDate?,
    @field:Size(max = 120) val vetName: String?,
    @field:Size(max = 160) val clinicName: String?,
    @field:Size(max = 500) val notes: String?,
)

data class HealthRecordDto(
    val id: UUID,
    val type: HealthRecordType,
    val name: String,
    val administeredOn: LocalDate?,
    val dueOn: LocalDate?,
    val vetName: String?,
    val clinicName: String?,
    val notes: String?,
    val status: HealthStatus,
)

data class WeightRequest(
    @field:NotNull val measuredOn: LocalDate?,
    @field:NotNull @field:DecimalMin("0.1") @field:DecimalMax("200") val weightKg: BigDecimal?,
)

data class WeightDto(val measuredOn: LocalDate, val weightKg: BigDecimal)

data class HealthPassportDto(
    val petId: UUID,
    val petName: String,
    val species: String,
    val breed: String?,
    val records: List<HealthRecordDto>,
    val upcoming: List<HealthRecordDto>,
    val weights: List<WeightDto>,
)

data class ShareLinkRequest(@field:Min(1) @field:Max(30) val days: Int? = 7)

data class ShareLinkDto(val token: String, val path: String, val expiresAt: Instant)
