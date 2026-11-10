package com.pawmotionai.billing.adapter.out.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pawmotionai.billing.application.port.out.CheckoutDetails;
import com.pawmotionai.billing.application.port.out.CheckoutSession;
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent;
import com.pawmotionai.billing.application.port.out.WebhookRequest;
import com.pawmotionai.billing.domain.exception.PaymentGatewayException;
import com.pawmotionai.billing.domain.exception.WebhookVerificationException;
import com.pawmotionai.configuration.AbacatePayProperties;

@Component
@ConditionalOnProperty(name = "app.billing.provider", havingValue = "abacatepay")
public class AbacatePayHostedCheckoutAdapter implements PaymentGatewayPort {

    private final AbacatePayProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AbacatePayHostedCheckoutAdapter(AbacatePayProperties properties) {
        this.properties = properties;
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
            || properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("ABACATEPAY_API_KEY and ABACATEPAY_WEBHOOK_SECRET are required when PAYMENT_PROVIDER=abacatepay");
        }
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public String provider() {
        return "abacatepay";
    }

    @Override
    public CheckoutSession createCheckout(CreateCheckoutRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("partnerId", request.partnerId().toString());
        metadata.put("transactionId", request.transactionId().toString());
        Map<String, Object> body = new HashMap<>();
        body.put("items", java.util.List.of(Map.of(
            "id", properties.getCreditProductExternalId(),
            "quantity", request.amount().stripTrailingZeros().intValueExact())));
        body.put("methods", java.util.List.of("PIX", "CARD"));
        body.put("externalId", request.transactionId().toString());
        body.put("metadata", metadata);
        body.put("completionUrl", request.completionUrl());
        body.put("returnUrl", request.returnUrl());
        if (request.customerId() != null && !request.customerId().isBlank()) {
            body.put("customerId", request.customerId());
        }
        JsonNode data = post("/checkouts/create", body).path("data");
        String checkoutId = data.path("id").asText(null);
        String checkoutUrl = data.path("url").asText(null);
        if (checkoutId == null || checkoutUrl == null) {
            throw new PaymentGatewayException("AbacatePay checkout response did not include id/url");
        }
        return new CheckoutSession(checkoutId, checkoutUrl);
    }

    @Override
    public CheckoutDetails fetchCheckoutByExternalId(String externalId) {
        JsonNode data = get("/checkouts/get?externalId=" + externalId).path("data");
        String status = data.path("status").asText(null);
        if (status == null) {
            throw new PaymentGatewayException("AbacatePay checkout response did not include status");
        }
        return new CheckoutDetails(data.path("id").asText(null), data.path("externalId").asText(externalId),
            status.toUpperCase(), paidAmount(data), "BRL");
    }

    @Override
    public void deleteCheckout(String providerCheckoutId) {
        post("/checkouts/delete", Map.of("id", providerCheckoutId));
    }

    @Override
    public ProviderWebhookEvent verifyWebhook(WebhookRequest request) throws WebhookVerificationException {
        String providedSecret = header(request.headers(), "x-webhook-secret");
        if (providedSecret == null || !constantTimeEquals(providedSecret, properties.getWebhookSecret())) {
            throw new WebhookVerificationException("Invalid webhook secret");
        }
        String signature = header(request.headers(), "x-webhook-signature");
        if (signature == null || !constantTimeEquals(signature, sign(request.body()))) {
            throw new WebhookVerificationException("Invalid webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(request.body());
            JsonNode data = root.path("data");
            String eventId = root.path("id").asText(null);
            String eventType = root.path("event").asText(null);
            if (eventId == null || eventType == null) {
                throw new WebhookVerificationException("Webhook envelope is missing id/event");
            }
            String externalId = firstText(data, "externalId", "transactionId");
            if (externalId == null) {
                externalId = firstText(data.path("metadata"), "transactionId");
            }
            String checkoutId = firstText(data, "id", "checkoutId");
            if (checkoutId == null) {
                checkoutId = firstText(data.path("checkout"), "id");
            }
            return new ProviderWebhookEvent(eventId, eventType, externalId, checkoutId,
                root.path("devMode").asBoolean(false));
        } catch (WebhookVerificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WebhookVerificationException("Invalid webhook payload");
        }
    }

    @Override
    public boolean expectedDevMode() {
        return properties.getApiKey().startsWith("abc_dev_");
    }

    private JsonNode post(String path, Object body) {
        try {
            JsonNode response = restClient.post()
                .uri(path)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
            return requireData(response);
        } catch (RestClientException exception) {
            throw new PaymentGatewayException("AbacatePay request failed: " + path, exception);
        }
    }

    private JsonNode get(String path) {
        try {
            JsonNode response = restClient.get()
                .uri(path)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(JsonNode.class);
            return requireData(response);
        } catch (RestClientException exception) {
            throw new PaymentGatewayException("AbacatePay request failed: " + path, exception);
        }
    }

    private JsonNode requireData(JsonNode response) {
        if (response == null || !response.path("success").asBoolean(true)) {
            throw new PaymentGatewayException("AbacatePay returned an unsuccessful response");
        }
        return response;
    }

    private BigDecimal paidAmount(JsonNode data) {
        for (String field : new String[]{"paidAmount", "amount", "value"}) {
            JsonNode node = data.path(field);
            if (node.isNumber()) {
                return BigDecimal.valueOf(node.asLong()).movePointLeft(2);
            }
        }
        return null;
    }

    private String sign(byte[] body) throws WebhookVerificationException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(body));
        } catch (Exception exception) {
            throw new WebhookVerificationException("Unable to verify webhook signature");
        }
    }

    private boolean constantTimeEquals(String provided, String expected) {
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    private String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}
