package com.pawmotionai.partner.application.port.in;

import java.util.List;

import com.pawmotionai.partner.application.dto.OwnerRecommendationDto;

public interface ListOwnerRecommendationsUseCase {

    List<OwnerRecommendationDto> listForOwners();
}
