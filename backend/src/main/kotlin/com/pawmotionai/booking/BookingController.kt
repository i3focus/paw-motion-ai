package com.pawmotionai.booking

import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.ownerId
import com.pawmotionai.shared.requireRole
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
@Tag(name = "Partners and bookings")
class BookingController(private val service: BookingService) {

    @GetMapping("/partners/{partnerId}")
    fun partner(@PathVariable partnerId: UUID): PartnerProfileDto = service.profile(partnerId)

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    fun book(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateBookingRequest): BookingDto =
        service.book(jwt.ownerId, request)

    @GetMapping("/bookings")
    fun myBookings(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<BookingDto> =
        service.ownerBookings(jwt.ownerId, PageRequest.of(page, size))

    @PostMapping("/bookings/{bookingId}/cancel")
    fun cancel(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookingId: UUID): BookingDto = service.cancel(jwt.ownerId, bookingId)

    @GetMapping("/partner/services")
    fun partnerServices(@AuthenticationPrincipal jwt: Jwt): List<PartnerServiceDto> {
        jwt.requireRole("PARTNER")
        return service.services(jwt.ownerId, activeOnly = false)
    }

    @PostMapping("/partner/services")
    @ResponseStatus(HttpStatus.CREATED)
    fun createService(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: PartnerServiceRequest): PartnerServiceDto {
        jwt.requireRole("PARTNER")
        return service.createService(jwt.ownerId, request)
    }

    @PutMapping("/partner/services/{serviceId}")
    fun updateService(@AuthenticationPrincipal jwt: Jwt, @PathVariable serviceId: UUID, @Valid @RequestBody request: PartnerServiceRequest): PartnerServiceDto {
        jwt.requireRole("PARTNER")
        return service.updateService(jwt.ownerId, serviceId, request)
    }

    @GetMapping("/partner/bookings")
    fun partnerBookings(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<BookingDto> {
        jwt.requireRole("PARTNER")
        return service.partnerBookings(jwt.ownerId, PageRequest.of(page, size))
    }

    @PostMapping("/partner/bookings/{bookingId}/confirm")
    fun confirm(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookingId: UUID): BookingDto {
        jwt.requireRole("PARTNER")
        return service.partnerTransition(jwt.ownerId, bookingId, BookingStatus.CONFIRMED)
    }

    @PostMapping("/partner/bookings/{bookingId}/complete")
    fun complete(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookingId: UUID): BookingDto {
        jwt.requireRole("PARTNER")
        return service.partnerTransition(jwt.ownerId, bookingId, BookingStatus.COMPLETED)
    }
}
