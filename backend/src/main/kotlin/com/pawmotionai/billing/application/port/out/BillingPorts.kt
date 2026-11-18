package com.pawmotionai.billing.application.port.out

import com.pawmotionai.billing.domain.model.PaymentTransaction
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CheckoutDetails(
    val providerCheckoutId: String?,
    val externalId: String?,
    val status: String?,
    val paidAmount: BigDecimal?,
    val currency: String?,
)

data class CheckoutSession(val providerCheckoutId: String, val checkoutUrl: String)

data class CreateCheckoutRequest(
    val partnerId: UUID,
    val transactionId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val expectedCredits: Long,
    val completionUrl: String,
    val returnUrl: String,
    val customerId: String?,
)

data class ProviderWebhookEvent(
    val providerEventId: String?,
    val eventType: String?,
    val externalId: String?,
    val checkoutId: String?,
    val devMode: Boolean,
)

data class WebhookRequest(val provider: String, val headers: Map<String, String>, val body: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is WebhookRequest && provider == other.provider
            && headers == other.headers && body.contentEquals(other.body))

    override fun hashCode(): Int = 31 * provider.hashCode() + body.contentHashCode()
}

interface PaymentGatewayPort {

    fun provider(): String

    fun createCheckout(request: CreateCheckoutRequest): CheckoutSession

    fun fetchCheckoutByExternalId(externalId: String): CheckoutDetails

    fun deleteCheckout(providerCheckoutId: String)

    fun verifyWebhook(request: WebhookRequest): ProviderWebhookEvent

    fun expectedDevMode(): Boolean
}

interface PaymentTransactionRepository {

    fun save(transaction: PaymentTransaction): PaymentTransaction

    fun findById(id: UUID): PaymentTransaction?

    fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String?): PaymentTransaction?

    fun findByProviderAndProviderCheckoutId(provider: String, providerCheckoutId: String): PaymentTransaction?

    fun findByPartnerIdOrderByCreatedAtDesc(partnerId: UUID): List<PaymentTransaction>

    fun findPendingCreatedBefore(createdBefore: Instant): List<PaymentTransaction>
}

interface PaymentWebhookEventRepository {

    fun exists(provider: String, providerEventId: String): Boolean

    fun save(provider: String, providerEventId: String, payloadHash: String, receivedAt: Instant)
}
