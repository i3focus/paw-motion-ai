package com.pawmotionai.billing.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.billing.domain.model.PaymentStatus;

public record PaymentTransactionResponseDto(UUID id, UUID partnerId, java.math.BigDecimal amount, String currency,
                                            long expectedCredits, String method, String provider, PaymentStatus status,
                                            String providerCheckoutUrl, String failureReason, Instant createdAt,
                                            Instant updatedAt) {
}
