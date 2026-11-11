package com.pawmotionai.community

import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Communities and events")
class CommunityController(private val service: CommunityService) {

    @GetMapping("/communities")
    fun communities(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) kind: CommunityKind?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<CommunityDto> = service.communities(jwt.ownerId, kind, q, PageRequest.of(page, size))

    @PostMapping("/communities")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateCommunityRequest): CommunityDto =
        service.create(jwt.ownerId, request)

    @GetMapping("/communities/{communityId}")
    fun community(@AuthenticationPrincipal jwt: Jwt, @PathVariable communityId: UUID): CommunityDto = service.community(communityId, jwt.ownerId)

    @PostMapping("/communities/{communityId}/join")
    fun join(@AuthenticationPrincipal jwt: Jwt, @PathVariable communityId: UUID): CommunityDto = service.join(jwt.ownerId, communityId, true)

    @DeleteMapping("/communities/{communityId}/join")
    fun leave(@AuthenticationPrincipal jwt: Jwt, @PathVariable communityId: UUID): CommunityDto = service.join(jwt.ownerId, communityId, false)

    @GetMapping("/communities/{communityId}/events")
    fun events(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable communityId: UUID,
        @RequestParam(required = false) upcoming: Boolean?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<CommunityEventDto> = service.events(jwt.ownerId, communityId, PageRequest.of(page, size), upcoming ?: true)

    @PostMapping("/communities/{communityId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(@AuthenticationPrincipal jwt: Jwt, @PathVariable communityId: UUID, @Valid @RequestBody request: CreateEventRequest): CommunityEventDto =
        service.createEvent(jwt.ownerId, communityId, request)

    @PostMapping("/events/{eventId}/rsvp")
    fun rsvp(@AuthenticationPrincipal jwt: Jwt, @PathVariable eventId: UUID, @Valid @RequestBody request: RsvpRequest): CommunityEventDto =
        service.rsvp(jwt.ownerId, eventId, request.status)

    @GetMapping("/discover")
    fun discover(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) lat: Double?, @RequestParam(required = false) lng: Double?): DiscoverDto =
        service.discover(jwt.ownerId, lat, lng)
}
