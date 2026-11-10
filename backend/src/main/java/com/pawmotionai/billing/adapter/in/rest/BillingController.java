package com.pawmotionai.billing.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.application.dto.PricingResponseDto;
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand;
import com.pawmotionai.billing.application.port.in.CreatePurchaseUseCase;
import com.pawmotionai.billing.application.port.in.GetBillingPricingUseCase;
import com.pawmotionai.billing.application.port.in.GetPurchaseUseCase;
import com.pawmotionai.billing.application.port.in.ListPurchasesUseCase;

@RestController
@RequestMapping("/api/partner/billing")
public class BillingController {

    private final GetBillingPricingUseCase pricingUseCase;
    private final CreatePurchaseUseCase createPurchaseUseCase;
    private final ListPurchasesUseCase listPurchasesUseCase;
    private final GetPurchaseUseCase getPurchaseUseCase;

    public BillingController(GetBillingPricingUseCase pricingUseCase, CreatePurchaseUseCase createPurchaseUseCase,
                             ListPurchasesUseCase listPurchasesUseCase, GetPurchaseUseCase getPurchaseUseCase) {
        this.pricingUseCase = pricingUseCase;
        this.createPurchaseUseCase = createPurchaseUseCase;
        this.listPurchasesUseCase = listPurchasesUseCase;
        this.getPurchaseUseCase = getPurchaseUseCase;
    }

    @GetMapping("/pricing")
    public PricingResponseDto pricing() {
        return pricingUseCase.pricing();
    }

    @PostMapping("/purchases")
    public ResponseEntity<PaymentTransactionResponseDto> createPurchase(@AuthenticationPrincipal Jwt jwt,
                                                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                        @Valid @RequestBody CreatePurchaseRequest request) {
        PaymentTransactionResponseDto response = createPurchaseUseCase.create(new PurchaseCreditsCommand(
            UUID.fromString(jwt.getSubject()), request.amount(), idempotencyKey, request.method(), request.customerId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/purchases")
    public List<PaymentTransactionResponseDto> listPurchases(@AuthenticationPrincipal Jwt jwt) {
        return listPurchasesUseCase.listByPartner(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/purchases/{transactionId}")
    public PaymentTransactionResponseDto getPurchase(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transactionId) {
        return getPurchaseUseCase.getByPartner(UUID.fromString(jwt.getSubject()), transactionId);
    }
}
