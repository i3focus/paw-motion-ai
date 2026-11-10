package com.pawmotionai.billing.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;

public interface ListPurchasesUseCase {

    List<PaymentTransactionResponseDto> listByPartner(UUID partnerId);
}
