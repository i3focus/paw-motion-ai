package com.pawmotionai.identity.adapter.`in`.rest

import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.dto.UpdateProfileCommand
import com.pawmotionai.identity.application.port.`in`.GetCurrentOwnerUseCase
import com.pawmotionai.identity.application.port.`in`.UpdateProfileUseCase
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/me")
class OwnerController(
    private val getCurrentOwnerUseCase: GetCurrentOwnerUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
) {

    @GetMapping
    fun currentOwner(@AuthenticationPrincipal jwt: Jwt): OwnerResponseDto =
        getCurrentOwnerUseCase.getCurrentOwner(UUID.fromString(jwt.subject))

    @PatchMapping
    fun updateProfile(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: UpdateProfileRequest): OwnerResponseDto =
        updateProfileUseCase.update(UUID.fromString(jwt.subject), UpdateProfileCommand(request.displayName,
            request.bio, request.phone, request.avatarUrl))
}
