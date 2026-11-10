package com.pawmotionai.billing.application.port.out;

import java.math.BigDecimal;

public record CheckoutDetails(String providerCheckoutId, String externalId, String status, BigDecimal paidAmount,
                              String currency) {
}
