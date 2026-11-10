package com.pawmotionai.billing.application.port.in;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand;

public interface CreatePurchaseUseCase {

    PaymentTransactionResponseDto create(PurchaseCreditsCommand command);
}
