package com.pawmotionai.billing.application.dto

import com.pawmotionai.billing.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentTransactionResponseDto(
    val id: UUID,
    val partnerId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val expectedCredits: Long,
    val method: String?,
    val provider: String,
    val status: PaymentStatus,
    val providerCheckoutUrl: String?,
    val failureReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PricingResponseDto(
    val currency: String,
    val creditsPerCurrencyUnit: BigDecimal,
    val minPurchaseAmount: BigDecimal,
    val maxPurchaseAmount: BigDecimal,
    val campaignPriceAmount: BigDecimal,
    val campaignCreationCostCredits: Long,
    val campaignDurationDays: Int,
    val provider: String,
)

data class PurchaseCreditsCommand(
    val partnerId: UUID,
    val amount: BigDecimal,
    val idempotencyKey: String?,
    val method: String?,
    val customerId: String?,
)
