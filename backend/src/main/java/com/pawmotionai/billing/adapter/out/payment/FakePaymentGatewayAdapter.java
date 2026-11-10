package com.pawmotionai.billing.adapter.out.payment;

import java.util.Map;
import java.util.Set;
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
    private final Set<String> paidTransactions = ConcurrentHashMap.newKeySet();

    public FakePaymentGatewayAdapter(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;
    }

    @Override
    public String provider() {
        return "fake";
    }

    @Override
    public CheckoutSession createCheckout(CreateCheckoutRequest request) {
        String checkoutId = "fake_" + request.transactionId();
        checkoutToTransaction.put(checkoutId, request.transactionId().toString());
        return new CheckoutSession(checkoutId,
            billingProperties.getPartnerPortalBaseUrl() + "/billing/fake?transactionId=" + request.transactionId());
    }

    @Override
    public CheckoutDetails fetchCheckoutByExternalId(String externalId) {
        String status = paidTransactions.contains(externalId) ? "PAID" : "PENDING";
        return new CheckoutDetails("fake_" + externalId, externalId, status, null, billingProperties.getCurrency());
    }

    @Override
    public void deleteCheckout(String providerCheckoutId) {
        checkoutToTransaction.remove(providerCheckoutId);
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
        try {
            JsonNode root = objectMapper.readTree(request.body());
            String eventId = root.path("id").asText(null);
            String event = root.path("event").asText("checkout.completed");
            JsonNode data = root.path("data");
            String checkoutId = data.path("checkoutId").asText(data.path("id").asText(null));
            markPaid(event, checkoutId, data);
            return new ProviderWebhookEvent(eventId, event, checkoutId, checkoutId, root.path("devMode").asBoolean(false));
        } catch (Exception exception) {
            throw new WebhookVerificationException("Invalid fake webhook payload");
        }
    }

    @Override
    public boolean expectedDevMode() {
        return true;
    }

    private void markPaid(String event, String checkoutId, JsonNode data) {
        if (event == null || !event.contains("completed") || checkoutId == null) {
            return;
        }
        String transactionId = data.path("transactionId").asText(data.path("externalId").asText(null));
        if (transactionId != null && !transactionId.isBlank()) {
            paidTransactions.add(transactionId);
        }
        String mapped = checkoutToTransaction.get(checkoutId);
        if (mapped != null) {
            paidTransactions.add(mapped);
        }
    }

    private String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }
}
