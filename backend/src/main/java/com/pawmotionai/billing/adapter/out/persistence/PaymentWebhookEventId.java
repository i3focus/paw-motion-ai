package com.pawmotionai.billing.adapter.out.persistence;

import java.io.Serializable;

public class PaymentWebhookEventId implements Serializable {

    private String provider;
    private String providerEventId;

    protected PaymentWebhookEventId() {
    }

    public PaymentWebhookEventId(String provider, String providerEventId) {
        this.provider = provider;
        this.providerEventId = providerEventId;
    }

    public String getProvider() { return provider; }
    public String getProviderEventId() { return providerEventId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PaymentWebhookEventId that)) return false;
        return provider.equals(that.provider) && providerEventId.equals(that.providerEventId);
    }

    @Override
    public int hashCode() {
        return 31 * provider.hashCode() + providerEventId.hashCode();
    }
}
