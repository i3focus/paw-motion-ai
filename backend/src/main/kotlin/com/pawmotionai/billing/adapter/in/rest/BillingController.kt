package com.pawmotionai.billing.adapter.`in`.rest

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto
import com.pawmotionai.billing.application.dto.PricingResponseDto
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand
import com.pawmotionai.billing.application.port.`in`.CreatePurchaseUseCase
import com.pawmotionai.billing.application.port.`in`.GetBillingPricingUseCase
import com.pawmotionai.billing.application.port.`in`.GetPurchaseUseCase
import com.pawmotionai.billing.application.port.`in`.ListPurchasesUseCase
import com.pawmotionai.billing.application.usecase.PaymentLifecycleService
import com.pawmotionai.billing.domain.model.PaymentStatus
import com.pawmotionai.configuration.BillingProperties
import com.pawmotionai.shared.SecurityRoles
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class CreatePurchaseRequest(
    @field:NotNull @field:DecimalMin("0.01") val amount: BigDecimal,
    val method: String?,
    val customerId: String?,
)

@RestController
@Secured(SecurityRoles.PARTNER)
@RequestMapping("/api/partner/billing")
class BillingController(
    private val pricingUseCase: GetBillingPricingUseCase,
    private val createPurchaseUseCase: CreatePurchaseUseCase,
    private val listPurchasesUseCase: ListPurchasesUseCase,
    private val getPurchaseUseCase: GetPurchaseUseCase,
    private val paymentLifecycleService: PaymentLifecycleService,
    private val billingProperties: BillingProperties,
) {

    @GetMapping("/pricing")
    fun pricing(): PricingResponseDto = pricingUseCase.pricing()

    @PostMapping("/purchases")
    fun createPurchase(@AuthenticationPrincipal jwt: Jwt,
                       @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
                       @Valid @RequestBody request: CreatePurchaseRequest): ResponseEntity<PaymentTransactionResponseDto> {
        val response = createPurchaseUseCase.create(PurchaseCreditsCommand(
            UUID.fromString(jwt.subject), request.amount, idempotencyKey, request.method, request.customerId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/purchases")
    fun listPurchases(@AuthenticationPrincipal jwt: Jwt): List<PaymentTransactionResponseDto> =
        listPurchasesUseCase.listByPartner(UUID.fromString(jwt.subject))

    @GetMapping("/purchases/{transactionId}")
    fun getPurchase(@AuthenticationPrincipal jwt: Jwt,
                    @PathVariable transactionId: UUID): PaymentTransactionResponseDto =
        getPurchaseUseCase.getByPartner(UUID.fromString(jwt.subject), transactionId)

    @PostMapping("/purchases/{transactionId}/fake-complete")
    fun completeFakePurchase(@AuthenticationPrincipal jwt: Jwt,
                             @PathVariable transactionId: UUID): PaymentTransactionResponseDto {
        if (!billingProperties.provider.equals("fake", ignoreCase = true)) {
            throw IllegalStateException("Fake checkout is only available when PAYMENT_PROVIDER=fake")
        }
        val partnerId = UUID.fromString(jwt.subject)
        val purchase = getPurchaseUseCase.getByPartner(partnerId, transactionId)
        if (!purchase.provider.equals("fake", ignoreCase = true)) {
            throw IllegalStateException("Purchase was not created by the fake provider")
        }
        if (purchase.status == PaymentStatus.CONFIRMED) {
            return purchase
        }
        check(purchase.status == PaymentStatus.PENDING) { "Fake checkout can only complete a pending purchase" }
        paymentLifecycleService.confirm(partnerId, transactionId)
        return getPurchaseUseCase.getByPartner(partnerId, transactionId)
    }
}
