package com.pawmotionai.billing.application.port.in;

import java.util.UUID;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;

public interface GetPurchaseUseCase {

    PaymentTransactionResponseDto getByPartner(UUID partnerId, UUID transactionId);
}
