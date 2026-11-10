package com.pawmotionai.billing.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.billing.application.port.in.ProcessPaymentWebhookUseCase;
import com.pawmotionai.billing.application.port.out.CheckoutDetails;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.application.port.out.PaymentWebhookEventRepository;
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent;
import com.pawmotionai.billing.application.port.out.WebhookRequest;
import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException;
import com.pawmotionai.billing.domain.exception.WebhookVerificationException;
import com.pawmotionai.billing.domain.model.PaymentTransaction;

@Service
public class ProcessPaymentWebhookService implements ProcessPaymentWebhookUseCase {

    private final List<PaymentGatewayPort> gateways;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentLifecycleService lifecycleService;

    public ProcessPaymentWebhookService(List<PaymentGatewayPort> gateways,
                                        PaymentWebhookEventRepository webhookEventRepository,
                                        PaymentTransactionRepository transactionRepository,
                                        PaymentLifecycleService lifecycleService) {
        this.gateways = gateways;
        this.webhookEventRepository = webhookEventRepository;
        this.transactionRepository = transactionRepository;
        this.lifecycleService = lifecycleService;
    }

    @Override
    @Transactional
    public void process(WebhookRequest request) {
        PaymentGatewayPort gateway = gateways.stream()
            .filter(candidate -> candidate.provider().equalsIgnoreCase(request.provider()))
            .findFirst()
            .orElseThrow(() -> new WebhookVerificationException("Unknown payment provider"));
        ProviderWebhookEvent event = gateway.verifyWebhook(request);
        if (event.devMode() != gateway.expectedDevMode()) {
            throw new WebhookVerificationException("Webhook devMode does not match environment");
        }
        if (webhookEventRepository.exists(gateway.provider(), event.providerEventId())) {
            return;
        }
        webhookEventRepository.save(gateway.provider(), event.providerEventId(), payloadHash(request.body()),
            java.time.Instant.now());

        PaymentTransaction transaction = findTransaction(gateway.provider(), event);
        if (transaction == null) {
            return;
        }
        CheckoutDetails details = gateway.fetchCheckoutByExternalId(transaction.getId().toString());
        String status = details.status() == null ? "" : details.status().toUpperCase();
        String eventType = event.eventType() == null ? "" : event.eventType();
        try {
            if (eventType.contains("completed") || status.equals("PAID") || status.equals("REDEEMED")) {
                lifecycleService.confirm(transaction.getId());
            } else if (eventType.contains("disputed") || status.equals("UNDER_DISPUTE")) {
                lifecycleService.dispute(transaction.getId());
            } else if (eventType.contains("refunded") || eventType.contains("lost") || status.equals("REFUNDED")) {
                lifecycleService.refund(transaction.getId(), "Payment " + eventType);
            } else if (status.equals("FAILED") || status.equals("CANCELED")) {
                lifecycleService.fail(transaction.getId(), "Provider status " + status);
            }
        } catch (InvalidPaymentStateException ignored) {
            // Out-of-order or duplicate provider events are acknowledged without changes.
        }
    }

    private PaymentTransaction findTransaction(String provider, ProviderWebhookEvent event) {
        if (event.externalId() != null) {
            try {
                UUID transactionId = UUID.fromString(event.externalId());
                var byId = transactionRepository.findById(transactionId);
                if (byId.isPresent() && byId.get().getProvider().equalsIgnoreCase(provider)) {
                    return byId.get();
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to checkout id lookup.
            }
        }
        if (event.checkoutId() != null) {
            return transactionRepository.findByProviderAndProviderCheckoutId(provider, event.checkoutId()).orElse(null);
        }
        return null;
    }

    private String payloadHash(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash webhook payload", exception);
        }
    }
}
