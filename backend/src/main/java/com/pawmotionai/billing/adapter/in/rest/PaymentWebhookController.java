package com.pawmotionai.billing.adapter.in.rest;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawmotionai.billing.application.port.in.ProcessPaymentWebhookUseCase;
import com.pawmotionai.billing.application.port.out.WebhookRequest;

@RestController
@RequestMapping("/api/webhooks/payments")
public class PaymentWebhookController {

    private final ProcessPaymentWebhookUseCase processPaymentWebhookUseCase;

    public PaymentWebhookController(ProcessPaymentWebhookUseCase processPaymentWebhookUseCase) {
        this.processPaymentWebhookUseCase = processPaymentWebhookUseCase;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(@PathVariable String provider,
                                        @RequestHeader HttpHeaders headers,
                                        @RequestBody byte[] body) {
        processPaymentWebhookUseCase.process(new WebhookRequest(provider, headers.toSingleValueMap(), body));
        return ResponseEntity.ok().build();
    }
}
