package com.pawmotionai.billing.adapter.in.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    String method,
    String customerId) {
}
