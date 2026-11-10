package com.pawmotionai.billing.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.application.port.in.ListPurchasesUseCase;
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;

@Service
public class ListPurchasesService implements ListPurchasesUseCase {

    private final PaymentTransactionRepository repository;

    public ListPurchasesService(PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponseDto> listByPartner(UUID partnerId) {
        return repository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
            .map(PaymentTransactionResponseDtoMapper::from)
            .toList();
    }
}
