package com.pawmotionai.billing.application.usecase

import com.pawmotionai.billing.application.port.`in`.ProcessPaymentWebhookUseCase
import com.pawmotionai.billing.application.port.out.CheckoutDetails
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository
import com.pawmotionai.billing.application.port.out.PaymentWebhookEventRepository
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent
import com.pawmotionai.billing.application.port.out.WebhookRequest
import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException
import com.pawmotionai.billing.domain.exception.PaymentGatewayException
import com.pawmotionai.billing.domain.exception.WebhookVerificationException
import com.pawmotionai.billing.domain.model.PaymentTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import java.util.Locale
import java.util.UUID

@Service
class ProcessPaymentWebhookService(
    private val gateways: List<PaymentGatewayPort>,
    private val webhookEventRepository: PaymentWebhookEventRepository,
    private val transactionRepository: PaymentTransactionRepository,
    private val lifecycleService: PaymentLifecycleService,
) : ProcessPaymentWebhookUseCase {

    @Transactional
    override fun process(request: WebhookRequest) {
        val gateway = gateways.firstOrNull { it.provider().equals(request.provider, ignoreCase = true) }
            ?: throw WebhookVerificationException("Unknown payment provider")
        val event = gateway.verifyWebhook(request)
        if (event.devMode != gateway.expectedDevMode()) {
            throw WebhookVerificationException("Webhook devMode does not match environment")
        }
        val providerEventId = event.providerEventId ?: throw WebhookVerificationException("Webhook event id is missing")
        if (webhookEventRepository.exists(gateway.provider(), providerEventId)) {
            return
        }
        val transaction = findTransaction(gateway.provider(), event)
        if (transaction == null) {
            webhookEventRepository.save(gateway.provider(), providerEventId, payloadHash(request.body),
                Instant.now())
            return
        }
        val details = gateway.fetchCheckoutByExternalId(transaction.id.toString())
        val status = details.status?.uppercase(Locale.ROOT) ?: ""
        val eventType = event.eventType ?: ""
        if (eventType.contains("completed") && status == "PENDING") {
            throw PaymentGatewayException("Provider checkout is not yet consistent for completed webhook")
        }
        try {
            when {
                status == "UNDER_DISPUTE" || status == "DISPUTED" ->
                    lifecycleService.dispute(transaction.partnerId, transaction.id)
                status == "REFUNDED" ->
                    lifecycleService.refund(transaction.partnerId, transaction.id,
                        "Payment ${eventType.ifBlank { status }}")
                status == "PAID" || status == "REDEEMED" -> {
                    requirePaidDetails(transaction, details)
                    lifecycleService.confirm(transaction.partnerId, transaction.id)
                }
                status == "FAILED" || status == "CANCELED" ->
                    lifecycleService.fail(transaction.id, "Provider status $status")
            }
        } catch (ignored: InvalidPaymentStateException) {
            // Out-of-order or duplicate provider events are acknowledged without changes.
        }
        webhookEventRepository.save(gateway.provider(), providerEventId, payloadHash(request.body),
            Instant.now())
    }

    private fun requirePaidDetails(transaction: PaymentTransaction, details: CheckoutDetails) {
        if (details.paidAmount != null && details.paidAmount.compareTo(transaction.amount) != 0) {
            throw WebhookVerificationException("Paid amount does not match purchase amount")
        }
        if (details.currency != null && !details.currency.equals(transaction.currency, ignoreCase = true)) {
            throw WebhookVerificationException("Paid currency does not match purchase currency")
        }
    }

    private fun findTransaction(provider: String, event: ProviderWebhookEvent): PaymentTransaction? {
        if (event.externalId != null) {
            try {
                val transactionId = UUID.fromString(event.externalId)
                val byId = transactionRepository.findById(transactionId)
                if (byId != null && byId.provider.equals(provider, ignoreCase = true)) {
                    return byId
                }
            } catch (ignored: IllegalArgumentException) {
                // Fall through to checkout id lookup.
            }
        }
        if (event.checkoutId != null) {
            return transactionRepository.findByProviderAndProviderCheckoutId(provider, event.checkoutId)
        }
        return null
    }

    private fun payloadHash(body: ByteArray): String = try {
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body))
    } catch (exception: Exception) {
        throw IllegalStateException("Unable to hash webhook payload", exception)
    }
}
