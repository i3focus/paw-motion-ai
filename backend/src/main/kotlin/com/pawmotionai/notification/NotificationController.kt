package com.pawmotionai.notification

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
import org.springframework.web.bind.annotation.PatchMapping
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
@Tag(name = "Notifications and devices")
class NotificationController(private val service: NotificationService) {

    @PostMapping("/devices/push-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun registerToken(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: PushTokenRequest) =
        service.registerToken(jwt.ownerId, request.token!!, request.platform!!)

    @DeleteMapping("/devices/push-tokens/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeToken(@AuthenticationPrincipal jwt: Jwt, @PathVariable token: String) = service.removeToken(jwt.ownerId, token)

    @GetMapping("/me/notification-preferences")
    fun preferences(@AuthenticationPrincipal jwt: Jwt): NotificationPreferencesDto = service.preferences(jwt.ownerId)

    @PatchMapping("/me/notification-preferences")
    fun updatePreferences(@AuthenticationPrincipal jwt: Jwt, @RequestBody prefs: NotificationPreferencesDto): NotificationPreferencesDto =
        service.updatePreferences(jwt.ownerId, prefs)

    @GetMapping("/notifications")
    fun notifications(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?): PageResponse<NotificationDto> =
        service.notifications(jwt.ownerId, PageRequest.of(page, size))

    @GetMapping("/notifications/unread-count")
    fun unreadCount(@AuthenticationPrincipal jwt: Jwt): Map<String, Long> = mapOf("unread" to service.unreadCount(jwt.ownerId))

    @PostMapping("/notifications/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun read(@AuthenticationPrincipal jwt: Jwt, @PathVariable notificationId: UUID) = service.markRead(jwt.ownerId, notificationId)

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readAll(@AuthenticationPrincipal jwt: Jwt) = service.markAllRead(jwt.ownerId)
}
