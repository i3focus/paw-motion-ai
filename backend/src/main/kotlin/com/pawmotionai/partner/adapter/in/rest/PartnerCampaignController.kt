package com.pawmotionai.partner.adapter.`in`.rest

import com.pawmotionai.partner.application.dto.CampaignResponseDto
import com.pawmotionai.partner.application.dto.CreateCampaignCommand
import com.pawmotionai.partner.application.port.`in`.CreateCampaignUseCase
import com.pawmotionai.shared.SecurityRoles
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateCampaignRequest(
    @field:NotBlank @field:Size(max = 160) val title: String,
    @field:NotBlank @field:Size(max = 2000) val description: String,
    @field:NotBlank @field:Size(max = 160) val targetAudience: String,
    @field:NotBlank @field:Size(max = 160) val callToAction: String,
)

@RestController
@Secured(SecurityRoles.PARTNER)
@RequestMapping("/api/partner/campaigns")
class PartnerCampaignController(private val createCampaignUseCase: CreateCampaignUseCase) {

    @PostMapping
    fun create(@AuthenticationPrincipal jwt: Jwt,
               @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
               @Valid @RequestBody request: CreateCampaignRequest): ResponseEntity<CampaignResponseDto> {
        val partnerId = UUID.fromString(jwt.subject)
        val response = createCampaignUseCase.create(partnerId,
            CreateCampaignCommand(partnerId, request.title, request.description,
                request.targetAudience, request.callToAction, idempotencyKey))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
