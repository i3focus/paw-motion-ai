package com.pawmotionai.billing.domain.model;

public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    EXPIRED,
    CANCELED,
    DISPUTED,
    REFUNDED
}
