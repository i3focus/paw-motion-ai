package com.pawmotionai.identity.adapter.in.rest;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.dto.UpdateProfileCommand;
import com.pawmotionai.identity.application.port.in.GetCurrentOwnerUseCase;
import com.pawmotionai.identity.application.port.in.UpdateProfileUseCase;

@RestController
@RequestMapping("/api/me")
public class OwnerController {

    private final GetCurrentOwnerUseCase getCurrentOwnerUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    public OwnerController(GetCurrentOwnerUseCase getCurrentOwnerUseCase, UpdateProfileUseCase updateProfileUseCase) {
        this.getCurrentOwnerUseCase = getCurrentOwnerUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    @GetMapping
    public OwnerResponseDto currentOwner(@AuthenticationPrincipal Jwt jwt) {
        return getCurrentOwnerUseCase.getCurrentOwner(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping
    public OwnerResponseDto updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return updateProfileUseCase.update(UUID.fromString(jwt.getSubject()), new UpdateProfileCommand(request.displayName(),
            request.bio(), request.phone(), request.avatarUrl(), request.businessName(), request.website(), request.partnerDescription(),
            request.latitude(), request.longitude(), Boolean.TRUE.equals(request.clearCoordinates())));
    }
}
