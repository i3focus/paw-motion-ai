package com.pawmotionai.identity.adapter.in.rest;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.port.in.GetCurrentOwnerUseCase;

@RestController
@RequestMapping("/api/me")
public class OwnerController {

    private final GetCurrentOwnerUseCase getCurrentOwnerUseCase;

    public OwnerController(GetCurrentOwnerUseCase getCurrentOwnerUseCase) {
        this.getCurrentOwnerUseCase = getCurrentOwnerUseCase;
    }

    @GetMapping
    public OwnerResponseDto currentOwner(@AuthenticationPrincipal Jwt jwt) {
        return getCurrentOwnerUseCase.getCurrentOwner(UUID.fromString(jwt.getSubject()));
    }
}
