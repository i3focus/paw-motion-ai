package com.pawmotionai.billing.application.usecase

import com.pawmotionai.billing.application.port.`in`.ReconcilePendingPaymentsUseCase
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository
import com.pawmotionai.billing.domain.exception.PaymentGatewayException
import com.pawmotionai.billing.domain.model.PaymentTransaction
import com.pawmotionai.configuration.BillingProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class ReconcilePendingPaymentsService(
    private val billingProperties: BillingProperties,
    private val transactionRepository: PaymentTransactionRepository,
    private val gateways: List<PaymentGatewayPort>,
    private val lifecycleService: PaymentLifecycleService,
) : ReconcilePendingPaymentsUseCase {

    @Scheduled(fixedDelayString = "\${app.billing.reconciliation-interval-ms:300000}", initialDelayString = "\${app.billing.reconciliation-initial-delay-ms:60000}")
    override fun reconcile() {
        val threshold = Instant.now().minus(billingProperties.pendingTtl.toMillis(), ChronoUnit.MILLIS)
        for (transaction in transactionRepository.findPendingCreatedBefore(threshold)) {
            try {
                reconcileOne(transaction)
            } catch (exception: Exception) {
                log.warn("Failed to reconcile payment transaction {}", transaction.id, exception)
            }
        }
    }

    private fun reconcileOne(transaction: PaymentTransaction) {
        val gateway = gateways.firstOrNull { it.provider().equals(transaction.provider, ignoreCase = true) }
            ?: return
        val details = gateway.fetchCheckoutByExternalId(transaction.id.toString())
        if (details.status != null && (details.status.equals("PAID", ignoreCase = true)
                || details.status.equals("REDEEMED", ignoreCase = true))) {
            lifecycleService.confirm(transaction.partnerId, transaction.id)
            return
        }
        if (transaction.providerCheckoutId != null) {
            try {
                gateway.deleteCheckout(transaction.providerCheckoutId!!)
            } catch (exception: PaymentGatewayException) {
                log.warn("Failed to delete expired checkout {}", transaction.providerCheckoutId, exception)
            }
        }
        lifecycleService.expire(transaction.id)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ReconcilePendingPaymentsService::class.java)
    }
}
