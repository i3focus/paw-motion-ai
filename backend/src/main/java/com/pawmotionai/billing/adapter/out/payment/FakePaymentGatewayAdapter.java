package com.pawmotionai.billing.adapter.out.payment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pawmotionai.billing.application.port.out.CheckoutDetails;
import com.pawmotionai.billing.application.port.out.CheckoutSession;
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.billing.application.port.out.ProviderWebhookEvent;
import com.pawmotionai.billing.application.port.out.WebhookRequest;
import com.pawmotionai.billing.domain.exception.WebhookVerificationException;
import com.pawmotionai.configuration.BillingProperties;

@Component
@ConditionalOnProperty(name = "app.billing.provider", havingValue = "fake")
public class FakePaymentGatewayAdapter implements PaymentGatewayPort {

    private final BillingProperties billingProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> checkoutToTransaction = new ConcurrentHashMap<>();
    private final Map<String, String> checkoutStates = new ConcurrentHashMap<>();

    public FakePaymentGatewayAdapter(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;
    }

    @Override
    public String provider() {
        return "fake";
    }

    @Override
    public CheckoutSession createCheckout(CreateCheckoutRequest request) {
        String transactionId = request.transactionId().toString();
        String checkoutId = "fake_" + transactionId;
        checkoutToTransaction.put(checkoutId, transactionId);
        checkoutStates.put(transactionId, "PENDING");
        return new CheckoutSession(checkoutId,
            billingProperties.getPartnerPortalBaseUrl() + "/billing/fake?transactionId=" + transactionId);
    }

    @Override
    public CheckoutDetails fetchCheckoutByExternalId(String externalId) {
        String status = checkoutStates.getOrDefault(externalId, "PENDING");
        return new CheckoutDetails("fake_" + externalId, externalId, status, null, billingProperties.getCurrency());
    }

    @Override
    public void deleteCheckout(String providerCheckoutId) {
        String transactionId = checkoutToTransaction.remove(providerCheckoutId);
        if (transactionId != null) {
            checkoutStates.remove(transactionId);
        }
    }

    @Override
    public ProviderWebhookEvent verifyWebhook(WebhookRequest request) throws WebhookVerificationException {
        String configuredSecret = billingProperties.getFakeWebhookSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new WebhookVerificationException("Fake webhook secret is not configured");
        }
        String providedSecret = header(request.headers(), "x-fake-webhook-secret");
        if (providedSecret == null || !providedSecret.equals(configuredSecret)) {
            throw new WebhookVerificationException("Invalid fake webhook secret");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(request.body());
        } catch (Exception exception) {
            throw new WebhookVerificationException("Invalid fake webhook payload");
        }
        boolean devMode = root.path("devMode").asBoolean(false);
        if (devMode != expectedDevMode()) {
            throw new WebhookVerificationException("Webhook devMode does not match environment");
        }
        String eventId = root.path("id").asText(null);
        String event = root.path("event").asText("checkout.completed");
        JsonNode data = root.path("data");
        String checkoutId = firstText(data, "checkoutId", "id");
        String transactionId = resolveTransactionId(data, checkoutId);
        applyEvent(event, transactionId);
        return new ProviderWebhookEvent(eventId, event, transactionId, checkoutId, devMode);
    }

    @Override
    public boolean expectedDevMode() {
        return true;
    }

    private String resolveTransactionId(JsonNode data, String checkoutId) {
        String explicit = firstText(data, "transactionId", "externalId");
        if (explicit != null) {
            return explicit;
        }
        String mapped = checkoutId == null ? null : checkoutToTransaction.get(checkoutId);
        if (mapped != null) {
            return mapped;
        }
        if (checkoutId != null && checkoutId.startsWith("fake_")) {
            return checkoutId.substring("fake_".length());
        }
        return null;
    }

    private void applyEvent(String event, String transactionId) {
        if (event == null || transactionId == null) {
            return;
        }
        String status = null;
        if (event.contains("completed")) {
            status = "PAID";
        } else if (event.contains("disputed")) {
            status = "UNDER_DISPUTE";
        } else if (event.contains("refunded") || event.contains("lost")) {
            status = "REFUNDED";
        } else if (event.contains("failed")) {
            status = "FAILED";
        } else if (event.contains("canceled") || event.contains("cancelled")) {
            status = "CANCELED";
        }
        if (status != null) {
            checkoutStates.put(transactionId, status);
        }
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

    private String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }
}
