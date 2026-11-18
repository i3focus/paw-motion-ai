package com.pawmotionai.billing.application.port.`in`

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto
import com.pawmotionai.billing.application.dto.PricingResponseDto
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand
import com.pawmotionai.billing.application.port.out.WebhookRequest
import java.util.UUID

interface CreatePurchaseUseCase {
    fun create(command: PurchaseCreditsCommand): PaymentTransactionResponseDto
}

interface GetBillingPricingUseCase {
    fun pricing(): PricingResponseDto
}

interface GetPurchaseUseCase {
    fun getByPartner(partnerId: UUID, transactionId: UUID): PaymentTransactionResponseDto
}

interface ListPurchasesUseCase {
    fun listByPartner(partnerId: UUID): List<PaymentTransactionResponseDto>
}

interface ProcessPaymentWebhookUseCase {
    fun process(request: WebhookRequest)
}

interface ReconcilePendingPaymentsUseCase {
    fun reconcile()
}
