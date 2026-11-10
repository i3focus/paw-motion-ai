package com.pawmotionai.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCheckoutRequest(UUID partnerId, UUID transactionId, BigDecimal amount, String currency,
                                    long expectedCredits, String completionUrl, String returnUrl, String customerId) {
}
