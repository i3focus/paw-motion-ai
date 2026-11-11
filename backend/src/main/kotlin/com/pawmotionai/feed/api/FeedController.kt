package com.pawmotionai.feed.api

import com.pawmotionai.feed.application.FeedService
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Feed")
class FeedController(private val service: FeedService) {

    @GetMapping("/feed")
    @Operation(summary = "Ranked home feed with at most one sponsored card per five organic cards")
    fun feed(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): PageResponse<FeedCardDto> = service.feed(jwt.ownerId, PageRequest.of(page, size), lat, lng)

    @PutMapping("/feed/{itemId}/reaction")
    fun react(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID, @Valid @RequestBody request: ReactRequest): ReactionSummaryDto =
        service.react(jwt.ownerId, itemId, request.type!!)

    @DeleteMapping("/feed/{itemId}/reaction")
    fun unreact(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID): ReactionSummaryDto =
        service.unreact(jwt.ownerId, itemId)

    @GetMapping("/feed/{itemId}/comments")
    fun comments(
        @PathVariable itemId: UUID,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<CommentDto> = service.comments(itemId, PageRequest.of(page, size))

    @PostMapping("/feed/{itemId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun comment(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID, @Valid @RequestBody request: CommentRequest): CommentDto =
        service.addComment(jwt.ownerId, itemId, request.body!!)

    @PostMapping("/ads/{campaignId}/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Record a sponsored impression or click; repeated impressions within 30 minutes are ignored")
    fun adEvent(@AuthenticationPrincipal jwt: Jwt, @PathVariable campaignId: UUID, @Valid @RequestBody request: AdEventRequest) =
        service.recordAdEvent(jwt.ownerId, campaignId, request.type!!, request.placement!!)
}
