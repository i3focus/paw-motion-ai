package com.pawmotionai.billing.application.port.in;

import com.pawmotionai.billing.application.dto.PricingResponseDto;

public interface GetBillingPricingUseCase {

    PricingResponseDto pricing();
}
