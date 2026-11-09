package com.pawmotionai.aiinsights.adapter.in.rest;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.aiinsights.application.dto.GenerateInsightCommand;
import com.pawmotionai.aiinsights.application.dto.InsightResponseDto;
import com.pawmotionai.aiinsights.application.port.in.GenerateInsightUseCase;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private static final int DEFAULT_PERIOD_DAYS = 7;

    private final GenerateInsightUseCase generateInsightUseCase;

    public InsightController(GenerateInsightUseCase generateInsightUseCase) {
        this.generateInsightUseCase = generateInsightUseCase;
    }

    @PostMapping
    public InsightResponseDto generateInsight(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody(required = false) GenerateInsightRequest request) {
        UUID petId = request == null ? null : request.petId();
        int periodDays = request == null || request.periodDays() == null ? DEFAULT_PERIOD_DAYS : request.periodDays();
        return generateInsightUseCase.generateInsight(new GenerateInsightCommand(ownerId(jwt), petId, periodDays));
    }

    private UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
