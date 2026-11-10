package com.pawmotionai.billing.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pawmotionai.billing.application.port.in.ReconcilePendingPaymentsUseCase;
import com.pawmotionai.billing.application.port.out.CheckoutDetails;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.domain.exception.PaymentGatewayException;
import com.pawmotionai.billing.domain.model.PaymentTransaction;
import com.pawmotionai.configuration.BillingProperties;

@Service
public class ReconcilePendingPaymentsService implements ReconcilePendingPaymentsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcilePendingPaymentsService.class);

    private final BillingProperties billingProperties;
    private final PaymentTransactionRepository transactionRepository;
    private final List<PaymentGatewayPort> gateways;
    private final PaymentLifecycleService lifecycleService;

    public ReconcilePendingPaymentsService(BillingProperties billingProperties,
                                           PaymentTransactionRepository transactionRepository,
                                           List<PaymentGatewayPort> gateways,
                                           PaymentLifecycleService lifecycleService) {
        this.billingProperties = billingProperties;
        this.transactionRepository = transactionRepository;
        this.gateways = gateways;
        this.lifecycleService = lifecycleService;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.billing.reconciliation-interval-ms:300000}", initialDelayString = "${app.billing.reconciliation-initial-delay-ms:60000}")
    public void reconcile() {
        Instant threshold = Instant.now().minus(billingProperties.getPendingTtl().toMillis(), ChronoUnit.MILLIS);
        for (PaymentTransaction transaction : transactionRepository.findPendingCreatedBefore(threshold)) {
            try {
                reconcileOne(transaction);
            } catch (Exception exception) {
                log.warn("Failed to reconcile payment transaction {}", transaction.getId(), exception);
            }
        }
    }

    private void reconcileOne(PaymentTransaction transaction) {
        PaymentGatewayPort gateway = gateways.stream()
            .filter(candidate -> candidate.provider().equalsIgnoreCase(transaction.getProvider()))
            .findFirst()
            .orElse(null);
        if (gateway == null) {
            return;
        }
        CheckoutDetails details = gateway.fetchCheckoutByExternalId(transaction.getId().toString());
        if (details.status() != null && (details.status().equalsIgnoreCase("PAID") || details.status().equalsIgnoreCase("REDEEMED"))) {
            lifecycleService.confirm(transaction.getPartnerId(), transaction.getId());
            return;
        }
        if (transaction.getProviderCheckoutId() != null) {
            try {
                gateway.deleteCheckout(transaction.getProviderCheckoutId());
            } catch (PaymentGatewayException exception) {
                log.warn("Failed to delete expired checkout {}", transaction.getProviderCheckoutId(), exception);
            }
        }
        lifecycleService.expire(transaction.getId());
    }
}
