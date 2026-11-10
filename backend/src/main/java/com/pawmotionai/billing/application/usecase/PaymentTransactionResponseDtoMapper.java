package com.pawmotionai.billing.application.usecase;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.domain.model.PaymentTransaction;

final class PaymentTransactionResponseDtoMapper {

    private PaymentTransactionResponseDtoMapper() {
    }

    static PaymentTransactionResponseDto from(PaymentTransaction transaction) {
        return new PaymentTransactionResponseDto(transaction.getId(), transaction.getPartnerId(), transaction.getAmount(),
            transaction.getCurrency(), transaction.getExpectedCredits(), transaction.getMethod(), transaction.getProvider(),
            transaction.getStatus(), transaction.getProviderCheckoutUrl(), transaction.getFailureReason(),
            transaction.getCreatedAt(), transaction.getUpdatedAt());
    }
}
