package com.pawmotionai.billing.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseCreditsCommand(UUID partnerId, BigDecimal amount, String idempotencyKey, String method,
                                     String customerId) {
}
