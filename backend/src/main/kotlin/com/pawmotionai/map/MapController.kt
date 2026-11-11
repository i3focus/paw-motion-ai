package com.pawmotionai.map

import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
@Tag(name = "Map")
class MapController(private val service: MapService) {

    @GetMapping("/places")
    @Operation(summary = "Places within a radius (meters, max 50 km), filtered by category and name")
    fun places(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) radius: Int?,
        @RequestParam(required = false) category: Set<PlaceCategory>?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<PlaceDto> = service.nearbyPlaces(lat, lng, radius, category.orEmpty(), q, PageRequest.of(page, size))

    @GetMapping("/places/{placeId}")
    fun place(@PathVariable placeId: UUID, @RequestParam(required = false) lat: Double?, @RequestParam(required = false) lng: Double?): PlaceDto =
        service.place(placeId, lat, lng)

    @PostMapping("/places/{placeId}/check-ins")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkIn(@AuthenticationPrincipal jwt: Jwt, @PathVariable placeId: UUID, @Valid @RequestBody request: CheckInRequest): CheckInDto =
        service.checkIn(jwt.ownerId, placeId, request.petId!!)

    @GetMapping("/lost-pets")
    fun lostPets(@RequestParam lat: Double, @RequestParam lng: Double): List<LostPetAlertDto> = service.nearbyLostPets(lat, lng)

    @PostMapping("/lost-pets")
    @ResponseStatus(HttpStatus.CREATED)
    fun createLostPet(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateLostPetAlertRequest): LostPetAlertDto =
        service.createLostPetAlert(jwt.ownerId, request)

    @PostMapping("/lost-pets/{alertId}/sightings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sighting(@AuthenticationPrincipal jwt: Jwt, @PathVariable alertId: UUID, @Valid @RequestBody request: SightingRequest) =
        service.reportSighting(jwt.ownerId, alertId, request)

    @PostMapping("/lost-pets/{alertId}/resolve")
    fun resolve(@AuthenticationPrincipal jwt: Jwt, @PathVariable alertId: UUID, @Valid @RequestBody request: ResolveAlertRequest): LostPetAlertDto =
        service.resolveAlert(jwt.ownerId, alertId, request.found!!)

    @GetMapping("/playdates")
    fun playdates(@AuthenticationPrincipal jwt: Jwt): List<PlaydateDto> = service.myPlaydates(jwt.ownerId)

    @PostMapping("/playdates")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlaydate(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreatePlaydateRequest): PlaydateDto =
        service.createPlaydate(jwt.ownerId, request)

    @PostMapping("/playdates/{playdateId}/respond")
    fun respond(@AuthenticationPrincipal jwt: Jwt, @PathVariable playdateId: UUID, @Valid @RequestBody request: RespondInviteRequest): PlaydateDto =
        service.respondToInvite(jwt.ownerId, playdateId, request.petId!!, request.response!!)

    @GetMapping("/pets/nearby")
    @Operation(summary = "Pets near a point; distances are rounded to 100 m and exact owner locations are never returned")
    fun nearbyPets(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) radius: Int?,
    ): List<NearbyPetDto> = service.nearbyPets(jwt.ownerId, lat, lng, radius)
}
