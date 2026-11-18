package com.pawmotionai.activity.adapter.`in`.rest

import com.pawmotionai.activity.application.dto.ActivityResponseDto
import com.pawmotionai.activity.application.dto.DashboardSummaryDto
import com.pawmotionai.activity.application.dto.RecordActivityCommand
import com.pawmotionai.activity.application.port.`in`.GetDashboardSummaryUseCase
import com.pawmotionai.activity.application.port.`in`.ListActivitiesUseCase
import com.pawmotionai.activity.application.port.`in`.RecordActivityUseCase
import com.pawmotionai.shared.SecurityRoles
import com.pawmotionai.shared.ownerId
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class CreateActivityRequest(
    @field:NotNull val petId: UUID,
    @field:NotBlank val type: String,
    @field:NotNull val startedAt: Instant,
    @field:Min(1) val durationSeconds: Long,
    @field:DecimalMin("0") val distanceMeters: Double?,
    @field:Size(max = 500) val notes: String?,
    @field:DecimalMin("-90") @field:DecimalMax("90") val latitude: Double?,
    @field:DecimalMin("-180") @field:DecimalMax("180") val longitude: Double?,
    val shareToPawCity: Boolean?,
)

@RestController
@Secured(SecurityRoles.OWNER)
@RequestMapping("/api")
class ActivityController(
    private val recordActivityUseCase: RecordActivityUseCase,
    private val listActivitiesUseCase: ListActivitiesUseCase,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
) {

    @PostMapping("/activities")
    fun recordActivity(@AuthenticationPrincipal jwt: Jwt,
                       @Valid @RequestBody request: CreateActivityRequest): ResponseEntity<ActivityResponseDto> {
        val response = recordActivityUseCase.recordActivity(RecordActivityCommand(
            jwt.ownerId, request.petId, request.type, request.startedAt, request.durationSeconds,
            request.distanceMeters, request.notes, request.latitude, request.longitude, request.shareToPawCity))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/activities")
    fun listActivities(@AuthenticationPrincipal jwt: Jwt,
                       @RequestParam(required = false) petId: UUID?,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
    ): List<ActivityResponseDto> = listActivitiesUseCase.listActivities(jwt.ownerId, petId, from, to)

    @GetMapping("/dashboard/summary")
    fun dashboardSummary(@AuthenticationPrincipal jwt: Jwt): DashboardSummaryDto =
        getDashboardSummaryUseCase.getSummary(jwt.ownerId)
}
