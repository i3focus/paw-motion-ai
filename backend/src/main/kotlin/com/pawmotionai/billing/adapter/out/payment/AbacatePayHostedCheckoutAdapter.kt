package com.pawmotionai.billing.adapter.out.payment

import com.pawmotionai.billing.application.port.out.CheckoutDetails
import com.pawmotionai.billing.application.port.out.CheckoutSession
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent
import com.pawmotionai.billing.application.port.out.WebhookRequest
import com.pawmotionai.billing.domain.exception.PaymentGatewayException
import com.pawmotionai.billing.domain.exception.WebhookVerificationException
import com.pawmotionai.configuration.AbacatePayProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@ConditionalOnProperty(name = ["app.billing.provider"], havingValue = "abacatepay")
class AbacatePayHostedCheckoutAdapter(properties: AbacatePayProperties) : PaymentGatewayPort {

    private val properties: AbacatePayProperties
    private val restClient: RestClient
    private val objectMapper = ObjectMapper()

    init {
        if (properties.apiKey.isNullOrBlank() || properties.webhookSecret.isNullOrBlank()) {
            throw IllegalStateException(
                "ABACATEPAY_API_KEY and ABACATEPAY_WEBHOOK_SECRET are required when PAYMENT_PROVIDER=abacatepay")
        }
        this.properties = properties
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl).build()
    }

    override fun provider(): String = "abacatepay"

    override fun createCheckout(request: CreateCheckoutRequest): CheckoutSession {
        val metadata = mutableMapOf<String, Any>(
            "partnerId" to request.partnerId.toString(),
            "transactionId" to request.transactionId.toString())
        val body = mutableMapOf<String, Any>(
            "items" to listOf(mapOf(
                "id" to properties.creditProductExternalId,
                "quantity" to request.amount.stripTrailingZeros().intValueExact())),
            "methods" to listOf("PIX", "CARD"),
            "externalId" to request.transactionId.toString(),
            "metadata" to metadata,
            "completionUrl" to request.completionUrl,
            "returnUrl" to request.returnUrl)
        if (!request.customerId.isNullOrBlank()) {
            body["customerId"] = request.customerId
        }
        val data = post("/checkouts/create", body).path("data")
        val checkoutId = data.path("id").asText(null)
        val checkoutUrl = data.path("url").asText(null)
        if (checkoutId == null || checkoutUrl == null) {
            throw PaymentGatewayException("AbacatePay checkout response did not include id/url")
        }
        return CheckoutSession(checkoutId, checkoutUrl)
    }

    override fun fetchCheckoutByExternalId(externalId: String): CheckoutDetails {
        val data = get("/checkouts/get?externalId=$externalId").path("data")
        val status = data.path("status").asText(null)
            ?: throw PaymentGatewayException("AbacatePay checkout response did not include status")
        return CheckoutDetails(data.path("id").asText(null), data.path("externalId").asText(externalId),
            status.uppercase(), paidAmount(data), "BRL")
    }

    override fun deleteCheckout(providerCheckoutId: String) {
        post("/checkouts/delete", mapOf("id" to providerCheckoutId))
    }

    override fun verifyWebhook(request: WebhookRequest): ProviderWebhookEvent {
        val providedSecret = header(request.headers, "x-webhook-secret")
        if (providedSecret == null || !constantTimeEquals(providedSecret, properties.webhookSecret)) {
            throw WebhookVerificationException("Invalid webhook secret")
        }
        val signature = header(request.headers, "x-webhook-signature")
        if (signature == null || !constantTimeEquals(signature, sign(request.body))) {
            throw WebhookVerificationException("Invalid webhook signature")
        }
        try {
            val root = objectMapper.readTree(request.body)
            val data = root.path("data")
            val eventId = root.path("id").asText(null)
            val eventType = root.path("event").asText(null)
            if (eventId == null || eventType == null) {
                throw WebhookVerificationException("Webhook envelope is missing id/event")
            }
            var externalId = firstText(data, "externalId", "transactionId")
            if (externalId == null) {
                externalId = firstText(data.path("metadata"), "transactionId")
            }
            var checkoutId = firstText(data, "id", "checkoutId")
            if (checkoutId == null) {
                checkoutId = firstText(data.path("checkout"), "id")
            }
            return ProviderWebhookEvent(eventId, eventType, externalId, checkoutId,
                root.path("devMode").asBoolean(false))
        } catch (exception: WebhookVerificationException) {
            throw exception
        } catch (exception: Exception) {
            throw WebhookVerificationException("Invalid webhook payload")
        }
    }

    override fun expectedDevMode(): Boolean = properties.apiKey.startsWith("abc_dev_")

    private fun post(path: String, body: Any): JsonNode = try {
        val response = restClient.post()
            .uri(path)
            .header("Authorization", "Bearer ${properties.apiKey}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode::class.java)
        requireData(response)
    } catch (exception: RestClientException) {
        throw PaymentGatewayException("AbacatePay request failed: $path", exception)
    }

    private fun get(path: String): JsonNode = try {
        val response = restClient.get()
            .uri(path)
            .header("Authorization", "Bearer ${properties.apiKey}")
            .retrieve()
            .body(JsonNode::class.java)
        requireData(response)
    } catch (exception: RestClientException) {
        throw PaymentGatewayException("AbacatePay request failed: $path", exception)
    }

    private fun requireData(response: JsonNode?): JsonNode {
        if (response == null || !response.path("success").asBoolean(true)) {
            throw PaymentGatewayException("AbacatePay returned an unsuccessful response")
        }
        return response
    }

    private fun paidAmount(data: JsonNode): BigDecimal? {
        for (field in arrayOf("paidAmount", "amount", "value")) {
            val node = data.path(field)
            if (node.isNumber) {
                return BigDecimal.valueOf(node.asLong()).movePointLeft(2)
            }
        }
        return null
    }

    private fun sign(body: ByteArray): String = try {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(ABACATEPAY_WEBHOOK_PUBLIC_KEY.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        Base64.getEncoder().encodeToString(mac.doFinal(body))
    } catch (exception: Exception) {
        throw WebhookVerificationException("Unable to verify webhook signature")
    }

    private fun constantTimeEquals(provided: String, expected: String): Boolean =
        MessageDigest.isEqual(provided.toByteArray(StandardCharsets.UTF_8), expected.toByteArray(StandardCharsets.UTF_8))

    private fun header(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun firstText(node: JsonNode, vararg fields: String): String? {
        for (field in fields) {
            val value = node.path(field)
            if (value.isTextual && !value.asText().isBlank()) {
                return value.asText()
            }
        }
        return null
    }

    private companion object {
        const val ABACATEPAY_WEBHOOK_PUBLIC_KEY =
            "t9dXRhHHo3yDEj5pVDYz0frf7q6bMKyMRmxxCPIPp3RCplBfXRxqlC6ZpiWmOqj4L63qEaeUOtrCI8P0VMUgo6iIga2ri9ogaHFs0WIIywSMg0q7RmBfybe1E5XJcfC4IW3alNqym0tXoAKkzvfEjZxV6bE0oG2zJrNNYmUCKZyV0KZ3JS8Votf9EAWWYdiDkMkpbMdPggfh1EqHlVkMiTady6jOR3hyzGEHrIz2Ret0xHKMbiqkr9HS1JhNHDX9"
    }
}
