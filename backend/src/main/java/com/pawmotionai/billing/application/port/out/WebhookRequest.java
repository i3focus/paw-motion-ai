package com.pawmotionai.billing.application.port.out;

import java.util.Map;

public record WebhookRequest(String provider, Map<String, String> headers, byte[] body) {
}
