package com.pawmotionai.ads

import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import com.pawmotionai.shared.requireRole
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Partner campaigns v2 and profile")
class AdsController(private val service: AdsService) {

    @GetMapping("/partner/campaigns/{campaignId}")
    fun campaign(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): CampaignV2Dto {
        jwt.requireRole("PARTNER", "ADMIN")
        return service.campaign(jwt.ownerId, campaignId)
    }

    @PatchMapping("/partner/campaigns/{campaignId}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID, @Valid @RequestBody request: UpdateCampaignRequest): CampaignV2Dto {
        jwt.requireRole("PARTNER")
        return service.update(jwt.ownerId, campaignId, request)
    }

    @PostMapping("/partner/campaigns/{campaignId}/submit")
    fun submit(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): CampaignV2Dto {
        jwt.requireRole("PARTNER")
        return service.submit(jwt.ownerId, campaignId)
    }

    @PostMapping("/partner/campaigns/{campaignId}/pause")
    fun pause(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): CampaignV2Dto {
        jwt.requireRole("PARTNER")
        return service.pause(jwt.ownerId, campaignId)
    }

    @PostMapping("/partner/campaigns/{campaignId}/resume")
    fun resume(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): CampaignV2Dto {
        jwt.requireRole("PARTNER")
        return service.resume(jwt.ownerId, campaignId)
    }

    @PostMapping("/partner/campaigns/{campaignId}/end")
    fun end(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): CampaignV2Dto {
        jwt.requireRole("PARTNER")
        return service.end(jwt.ownerId, campaignId)
    }

    @GetMapping("/partner/campaigns/{campaignId}/metrics")
    fun metrics(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable campaignId: UUID,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ): CampaignMetricsDto {
        jwt.requireRole("PARTNER")
        return service.metrics(jwt.ownerId, campaignId, from, to)
    }

    @GetMapping("/partner/profile")
    fun profile(@AuthenticationPrincipal jwt: Jwt): PartnerProfileDto {
        jwt.requireRole("PARTNER")
        return service.profile(jwt.ownerId)
    }

    @PatchMapping("/partner/profile")
    fun updateProfile(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: UpdatePartnerProfileRequest): PartnerProfileDto {
        jwt.requireRole("PARTNER")
        return service.upsertProfile(jwt.ownerId, request)
    }
}

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin moderation")
class AdminAdsController(private val service: AdsService) {

    @GetMapping("/campaigns")
    fun queue(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "IN_REVIEW") status: String,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<AdminCampaignDto> {
        jwt.requireRole("ADMIN")
        if (status !in setOf("IN_REVIEW", "ACTIVE", "PAUSED", "DRAFT", "ENDED")) throw BadRequestException("Unknown status")
        return service.reviewQueue(status, PageRequest.of(page, size))
    }

    @PostMapping("/campaigns/{campaignId}/approve")
    fun approve(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID): AdminCampaignDto {
        jwt.requireRole("ADMIN")
        return service.approve(jwt.ownerId, campaignId)
    }

    @PostMapping("/campaigns/{campaignId}/reject")
    fun reject(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID, @Valid @RequestBody(required = false) request: RejectCampaignRequest?): AdminCampaignDto {
        jwt.requireRole("ADMIN")
        return service.reject(jwt.ownerId, campaignId, request?.reason)
    }

    @PostMapping("/partners/{partnerId}/verify")
    fun verify(@AuthenticationPrincipal jwt: Jwt, @PathVariable partnerId: UUID, @RequestParam(defaultValue = "true") verified: Boolean): PartnerProfileDto {
        jwt.requireRole("ADMIN")
        return service.verifyPartner(jwt.ownerId, partnerId, verified)
    }
}
