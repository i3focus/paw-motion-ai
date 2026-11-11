package com.pawmotionai.partner.application.port.in;

import java.util.List;

import com.pawmotionai.partner.application.dto.OwnerPromotionDto;

public interface ListOwnerPromotionsUseCase {

    List<OwnerPromotionDto> listActivePromotions();
}
