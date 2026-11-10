package com.pawmotionai.billing.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.application.port.in.GetPurchaseUseCase;
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException;
import com.pawmotionai.billing.domain.model.PaymentTransaction;

@Service
public class GetPurchaseService implements GetPurchaseUseCase {

    private final PaymentTransactionRepository repository;

    public GetPurchaseService(PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponseDto getByPartner(UUID partnerId, UUID transactionId) {
        PaymentTransaction transaction = repository.findById(transactionId)
            .orElseThrow(PaymentTransactionNotFoundException::new);
        if (!transaction.getPartnerId().equals(partnerId)) {
            throw new PaymentTransactionNotFoundException();
        }
        return PaymentTransactionResponseDtoMapper.from(transaction);
    }
}
