package com.pawmotionai.billing.adapter.out.payment

import com.pawmotionai.billing.application.port.out.CheckoutDetails
import com.pawmotionai.billing.application.port.out.CheckoutSession
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent
import com.pawmotionai.billing.application.port.out.WebhookRequest
import com.pawmotionai.billing.domain.exception.WebhookVerificationException
import com.pawmotionai.configuration.BillingProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(name = ["app.billing.provider"], havingValue = "fake")
class FakePaymentGatewayAdapter(private val billingProperties: BillingProperties) : PaymentGatewayPort {

    private val objectMapper = ObjectMapper()
    private val checkoutToTransaction = ConcurrentHashMap<String, String>()
    private val checkoutStates = ConcurrentHashMap<String, String>()

    override fun provider(): String = "fake"

    override fun createCheckout(request: CreateCheckoutRequest): CheckoutSession {
        val transactionId = request.transactionId.toString()
        val checkoutId = "fake_$transactionId"
        checkoutToTransaction[checkoutId] = transactionId
        checkoutStates[transactionId] = "PENDING"
        return CheckoutSession(checkoutId,
            "${billingProperties.partnerPortalBaseUrl}/billing/fake?transactionId=$transactionId")
    }

    override fun fetchCheckoutByExternalId(externalId: String): CheckoutDetails {
        val status = checkoutStates.getOrDefault(externalId, "PENDING")
        return CheckoutDetails("fake_$externalId", externalId, status, null, billingProperties.currency)
    }

    override fun deleteCheckout(providerCheckoutId: String) {
        val transactionId = checkoutToTransaction.remove(providerCheckoutId)
        if (transactionId != null) {
            checkoutStates.remove(transactionId)
        }
    }

    override fun verifyWebhook(request: WebhookRequest): ProviderWebhookEvent {
        val configuredSecret = billingProperties.fakeWebhookSecret
        if (configuredSecret.isNullOrBlank()) {
            throw WebhookVerificationException("Fake webhook secret is not configured")
        }
        val providedSecret = header(request.headers, "x-fake-webhook-secret")
        if (providedSecret == null || providedSecret != configuredSecret) {
            throw WebhookVerificationException("Invalid fake webhook secret")
        }
        val root: JsonNode = try {
            objectMapper.readTree(request.body)
        } catch (exception: Exception) {
            throw WebhookVerificationException("Invalid fake webhook payload")
        }
        val devMode = root.path("devMode").asBoolean(false)
        if (devMode != expectedDevMode()) {
            throw WebhookVerificationException("Webhook devMode does not match environment")
        }
        val eventId = root.path("id").asText(null)
        val event = root.path("event").asText("checkout.completed")
        val data = root.path("data")
        val checkoutId = firstText(data, "checkoutId", "id")
        val transactionId = resolveTransactionId(data, checkoutId)
        applyEvent(event, transactionId)
        return ProviderWebhookEvent(eventId, event, transactionId, checkoutId, devMode)
    }

    override fun expectedDevMode(): Boolean = true

    private fun resolveTransactionId(data: JsonNode, checkoutId: String?): String? {
        val explicit = firstText(data, "transactionId", "externalId")
        if (explicit != null) {
            return explicit
        }
        val mapped = checkoutId?.let(checkoutToTransaction::get)
        if (mapped != null) {
            return mapped
        }
        if (checkoutId != null && checkoutId.startsWith("fake_")) {
            return checkoutId.substring("fake_".length)
        }
        return null
    }

    private fun applyEvent(event: String?, transactionId: String?) {
        if (event == null || transactionId == null) {
            return
        }
        val status = when {
            event.contains("completed") -> "PAID"
            event.contains("disputed") -> "UNDER_DISPUTE"
            event.contains("refunded") || event.contains("lost") -> "REFUNDED"
            event.contains("failed") -> "FAILED"
            event.contains("canceled") || event.contains("cancelled") -> "CANCELED"
            else -> null
        }
        if (status != null) {
            checkoutStates[transactionId] = status
        }
    }

    private fun firstText(node: JsonNode, vararg fields: String): String? {
        for (field in fields) {
            val value = node.path(field)
            if (value.isTextual && !value.asText().isBlank()) {
                return value.asText()
            }
        }
        return null
    }

    private fun header(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}
