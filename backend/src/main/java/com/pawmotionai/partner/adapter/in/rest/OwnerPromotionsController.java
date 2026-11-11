package com.pawmotionai.partner.adapter.in.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawmotionai.partner.application.dto.OwnerPromotionDto;
import com.pawmotionai.partner.application.port.in.ListOwnerPromotionsUseCase;

@RestController
@RequestMapping("/api/promotions")
public class OwnerPromotionsController {

    private final ListOwnerPromotionsUseCase listOwnerPromotionsUseCase;

    public OwnerPromotionsController(ListOwnerPromotionsUseCase listOwnerPromotionsUseCase) {
        this.listOwnerPromotionsUseCase = listOwnerPromotionsUseCase;
    }

    @GetMapping
    public List<OwnerPromotionDto> list() {
        return listOwnerPromotionsUseCase.listActivePromotions();
    }
}
