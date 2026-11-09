package com.pawmotionai.partner.adapter.in.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawmotionai.partner.application.dto.OwnerRecommendationDto;
import com.pawmotionai.partner.application.port.in.ListOwnerRecommendationsUseCase;

@RestController
@RequestMapping("/api/recommendations")
public class OwnerRecommendationsController {

    private final ListOwnerRecommendationsUseCase listOwnerRecommendationsUseCase;

    public OwnerRecommendationsController(ListOwnerRecommendationsUseCase listOwnerRecommendationsUseCase) {
        this.listOwnerRecommendationsUseCase = listOwnerRecommendationsUseCase;
    }

    @GetMapping
    public List<OwnerRecommendationDto> list() {
        return listOwnerRecommendationsUseCase.listForOwners();
    }
}
