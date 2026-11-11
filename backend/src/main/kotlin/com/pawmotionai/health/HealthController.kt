package com.pawmotionai.health

import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Health passport")
class HealthController(private val service: HealthService) {

    @GetMapping("/pets/{petId}/health")
    fun passport(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID): HealthPassportDto = service.passport(jwt.ownerId, petId)

    @PostMapping("/pets/{petId}/health/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun addRecord(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID, @Valid @RequestBody request: HealthRecordRequest): HealthRecordDto =
        service.addRecord(jwt.ownerId, petId, request)

    @PutMapping("/pets/{petId}/health/records/{recordId}")
    fun updateRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable petId: UUID,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: HealthRecordRequest,
    ): HealthRecordDto = service.updateRecord(jwt.ownerId, petId, recordId, request)

    @DeleteMapping("/pets/{petId}/health/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID, @PathVariable recordId: UUID) =
        service.deleteRecord(jwt.ownerId, petId, recordId)

    @PostMapping("/pets/{petId}/weights")
    fun weight(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID, @Valid @RequestBody request: WeightRequest): List<WeightDto> =
        service.recordWeight(jwt.ownerId, petId, request)

    @PostMapping("/pets/{petId}/health/share")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a read-only vet link; only a hash of the token is stored")
    fun share(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID, @Valid @RequestBody(required = false) request: ShareLinkRequest?): ShareLinkDto =
        service.createShareLink(jwt.ownerId, petId, request?.days ?: 7)

    @DeleteMapping("/pets/{petId}/health/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID) = service.revokeShareLinks(jwt.ownerId, petId)

    @GetMapping("/vet-share/{token}")
    @Operation(summary = "Public read-only health passport for a vet share link")
    fun shared(@PathVariable token: String): HealthPassportDto = service.sharedPassport(token)
}
