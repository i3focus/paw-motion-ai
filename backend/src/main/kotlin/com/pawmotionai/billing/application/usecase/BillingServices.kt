package com.pawmotionai.billing.application.usecase

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto
import com.pawmotionai.billing.application.dto.PricingResponseDto
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand
import com.pawmotionai.billing.application.port.`in`.CreatePurchaseUseCase
import com.pawmotionai.billing.application.port.`in`.GetBillingPricingUseCase
import com.pawmotionai.billing.application.port.`in`.GetPurchaseUseCase
import com.pawmotionai.billing.application.port.`in`.ListPurchasesUseCase
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository
import com.pawmotionai.billing.domain.exception.PaymentGatewayException
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException
import com.pawmotionai.billing.domain.model.PaymentTransaction
import com.pawmotionai.configuration.BillingProperties
import com.pawmotionai.configuration.CampaignPricingProperties
import com.pawmotionai.ledger.application.port.`in`.GetWalletUseCase
import com.pawmotionai.ledger.domain.exception.WalletBlockedException
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.WalletStatus
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

internal fun paymentTransactionDtoFrom(transaction: PaymentTransaction): PaymentTransactionResponseDto =
    PaymentTransactionResponseDto(transaction.id, transaction.partnerId, transaction.amount,
        transaction.currency, transaction.expectedCredits, transaction.method, transaction.provider,
        transaction.status, transaction.providerCheckoutUrl, transaction.failureReason,
        transaction.createdAt, transaction.updatedAt)

@Service
class BillingPricingService(
    private val billingProperties: BillingProperties,
    private val campaignPricingProperties: CampaignPricingProperties,
    private val gateways: List<PaymentGatewayPort>,
) : GetBillingPricingUseCase {

    @Transactional(readOnly = true)
    override fun pricing(): PricingResponseDto =
        PricingResponseDto(billingProperties.currency, billingProperties.creditsPerCurrencyUnit,
            billingProperties.minPurchaseAmount, billingProperties.maxPurchaseAmount,
            campaignPricingProperties.priceAmount, campaignPricingProperties.creationCostCredits,
            campaignPricingProperties.durationDays, billingProperties.provider)
}

@Service
class CreatePurchaseService(
    private val billingProperties: BillingProperties,
    private val transactionRepository: PaymentTransactionRepository,
    private val getWalletUseCase: GetWalletUseCase,
    private val gateways: List<PaymentGatewayPort>,
) : CreatePurchaseUseCase {

    override fun create(command: PurchaseCreditsCommand): PaymentTransactionResponseDto {
        validate(command)
        val gateway = gateway()
        val wallet = getWalletUseCase.getByPartner(command.partnerId)
        if (wallet.status == WalletStatus.BLOCKED) {
            throw WalletBlockedException(wallet.blockedReason)
        }
        val mainAccountId = wallet.accounts
            .firstOrNull { it.code == LedgerAccountCode.MAIN }
            ?.id
            ?: throw IllegalStateException("Partner wallet is missing MAIN account")

        val existingTransaction = transactionRepository.findByPartnerIdAndIdempotencyKey(
            command.partnerId, command.idempotencyKey)
        if (existingTransaction != null) {
            if (existingTransaction.amount.compareTo(command.amount) != 0
                || existingTransaction.method != command.method
                || existingTransaction.customerId != normalizeCustomerId(command.customerId)) {
                throw IllegalArgumentException("Idempotency key was already used for a different purchase")
            }
            return paymentTransactionDtoFrom(existingTransaction)
        }

        val amount = command.amount.setScale(2, RoundingMode.UNNECESSARY)
        val expectedCredits = amount.multiply(billingProperties.creditsPerCurrencyUnit)
            .setScale(0, RoundingMode.DOWN).longValueExact()
        val now = Instant.now()
        val transaction = try {
            transactionRepository.save(PaymentTransaction(UUID.randomUUID(), command.partnerId, amount,
                billingProperties.currency, billingProperties.creditsPerCurrencyUnit, expectedCredits,
                mainAccountId, command.method, gateway.provider(), command.idempotencyKey, now,
                normalizeCustomerId(command.customerId)))
        } catch (exception: DataIntegrityViolationException) {
            return transactionRepository.findByPartnerIdAndIdempotencyKey(command.partnerId, command.idempotencyKey)
                ?.let(::paymentTransactionDtoFrom)
                ?: throw exception
        }

        val completionUrl = "${billingProperties.partnerPortalBaseUrl}/billing/return?transactionId=${transaction.id}"
        val returnUrl = completionUrl
        return try {
            val session = gateway.createCheckout(CreateCheckoutRequest(command.partnerId, transaction.id, amount,
                billingProperties.currency, expectedCredits, completionUrl, returnUrl,
                normalizeCustomerId(command.customerId)))
            transaction.attachCheckout(session.providerCheckoutId, session.checkoutUrl, Instant.now())
            paymentTransactionDtoFrom(transactionRepository.save(transaction))
        } catch (exception: RuntimeException) {
            transaction.fail(exception.message, Instant.now())
            transactionRepository.save(transaction)
            throw PaymentGatewayException("Payment provider is unavailable", exception)
        }
    }

    private fun normalizeCustomerId(customerId: String?): String? = customerId?.trim()?.ifEmpty { null }

    private fun gateway(): PaymentGatewayPort =
        gateways.firstOrNull { it.provider().equals(billingProperties.provider, ignoreCase = true) }
            ?: throw PaymentGatewayException(
                "No payment gateway configured for provider ${billingProperties.provider}")

    private fun validate(command: PurchaseCreditsCommand?) {
        if (command == null || command.partnerId == null || command.amount == null
            || command.idempotencyKey.isNullOrBlank()) {
            throw IllegalArgumentException("Purchase is incomplete")
        }
        if (command.amount < billingProperties.minPurchaseAmount
            || command.amount > billingProperties.maxPurchaseAmount) {
            throw IllegalArgumentException("Purchase amount is outside the configured limits")
        }
        if (command.amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw IllegalArgumentException("Purchase amount must be a whole currency unit")
        }
    }
}

@Service
class GetPurchaseService(private val repository: PaymentTransactionRepository) : GetPurchaseUseCase {

    @Transactional(readOnly = true)
    override fun getByPartner(partnerId: UUID, transactionId: UUID): PaymentTransactionResponseDto {
        val transaction = repository.findById(transactionId)
            ?: throw PaymentTransactionNotFoundException()
        if (transaction.partnerId != partnerId) {
            throw PaymentTransactionNotFoundException()
        }
        return paymentTransactionDtoFrom(transaction)
    }
}

@Service
class ListPurchasesService(private val repository: PaymentTransactionRepository) : ListPurchasesUseCase {

    @Transactional(readOnly = true)
    override fun listByPartner(partnerId: UUID): List<PaymentTransactionResponseDto> =
        repository.findByPartnerIdOrderByCreatedAtDesc(partnerId).map(::paymentTransactionDtoFrom)
}
