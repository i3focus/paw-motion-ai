package com.pawmotionai.partner.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.dto.CreateCampaignCommand;
import com.pawmotionai.shared.CurrentUserKt;
import com.pawmotionai.partner.application.port.in.CreateCampaignUseCase;

@RestController
@RequestMapping("/api/partner/campaigns")
public class PartnerCampaignController {

    private final CreateCampaignUseCase createCampaignUseCase;

    public PartnerCampaignController(CreateCampaignUseCase createCampaignUseCase) {
        this.createCampaignUseCase = createCampaignUseCase;
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDto> create(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                      @Valid @RequestBody CreateCampaignRequest request) {
        CurrentUserKt.requireRole(jwt, "PARTNER");
        CampaignResponseDto response = createCampaignUseCase.create(UUID.fromString(jwt.getSubject()),
            new CreateCampaignCommand(UUID.fromString(jwt.getSubject()), request.title(), request.description(),
                request.targetAudience(), request.callToAction(), idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
