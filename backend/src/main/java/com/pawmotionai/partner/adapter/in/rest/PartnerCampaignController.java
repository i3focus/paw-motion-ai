package com.pawmotionai.partner.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.dto.CreateCampaignCommand;
import com.pawmotionai.partner.application.port.in.AnalyzeCampaignUseCase;
import com.pawmotionai.partner.application.port.in.CreateCampaignUseCase;
import com.pawmotionai.partner.application.port.in.ListCampaignsUseCase;

@RestController
@RequestMapping("/api/partner/campaigns")
public class PartnerCampaignController {

    private final CreateCampaignUseCase createCampaignUseCase;
    private final ListCampaignsUseCase listCampaignsUseCase;
    private final AnalyzeCampaignUseCase analyzeCampaignUseCase;

    public PartnerCampaignController(CreateCampaignUseCase createCampaignUseCase, ListCampaignsUseCase listCampaignsUseCase,
                                     AnalyzeCampaignUseCase analyzeCampaignUseCase) {
        this.createCampaignUseCase = createCampaignUseCase;
        this.listCampaignsUseCase = listCampaignsUseCase;
        this.analyzeCampaignUseCase = analyzeCampaignUseCase;
    }

    @GetMapping
    public List<CampaignResponseDto> list(@AuthenticationPrincipal Jwt jwt) {
        return listCampaignsUseCase.listByPartner(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDto> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponseDto response = createCampaignUseCase.create(UUID.fromString(jwt.getSubject()),
            new CreateCampaignCommand(UUID.fromString(jwt.getSubject()), request.title(), request.description(),
                request.targetAudience(), request.callToAction()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{campaignId}/analyze")
    public CampaignResponseDto analyze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID campaignId) {
        return analyzeCampaignUseCase.analyze(UUID.fromString(jwt.getSubject()), campaignId);
    }
}
