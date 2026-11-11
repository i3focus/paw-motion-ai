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
import com.pawmotionai.billing.application.usecase.PaymentLifecycleService;
import com.pawmotionai.billing.domain.model.PaymentStatus;
import com.pawmotionai.configuration.BillingProperties;

@RestController
@RequestMapping("/api/partner/billing")
public class BillingController {

    private final GetBillingPricingUseCase pricingUseCase;
    private final CreatePurchaseUseCase createPurchaseUseCase;
    private final ListPurchasesUseCase listPurchasesUseCase;
    private final GetPurchaseUseCase getPurchaseUseCase;
    private final PaymentLifecycleService paymentLifecycleService;
    private final BillingProperties billingProperties;

    public BillingController(GetBillingPricingUseCase pricingUseCase, CreatePurchaseUseCase createPurchaseUseCase,
                             ListPurchasesUseCase listPurchasesUseCase, GetPurchaseUseCase getPurchaseUseCase,
                             PaymentLifecycleService paymentLifecycleService, BillingProperties billingProperties) {
        this.pricingUseCase = pricingUseCase;
        this.createPurchaseUseCase = createPurchaseUseCase;
        this.listPurchasesUseCase = listPurchasesUseCase;
        this.getPurchaseUseCase = getPurchaseUseCase;
        this.paymentLifecycleService = paymentLifecycleService;
        this.billingProperties = billingProperties;
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

    @PostMapping("/purchases/{transactionId}/fake-complete")
    public PaymentTransactionResponseDto completeFakePurchase(@AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable UUID transactionId) {
        if (!billingProperties.getProvider().equalsIgnoreCase("fake")) {
            throw new IllegalStateException("Fake checkout is only available when PAYMENT_PROVIDER=fake");
        }
        UUID partnerId = UUID.fromString(jwt.getSubject());
        PaymentTransactionResponseDto purchase = getPurchaseUseCase.getByPartner(partnerId, transactionId);
        if (!"fake".equalsIgnoreCase(purchase.provider())) {
            throw new IllegalStateException("Purchase was not created by the fake provider");
        }
        if (purchase.status() == PaymentStatus.CONFIRMED) {
            return purchase;
        }
        if (purchase.status() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Fake checkout can only complete a pending purchase");
        }
        paymentLifecycleService.confirm(partnerId, transactionId);
        return getPurchaseUseCase.getByPartner(partnerId, transactionId);
    }
}
