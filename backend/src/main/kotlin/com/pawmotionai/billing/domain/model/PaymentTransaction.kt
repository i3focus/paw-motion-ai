package com.pawmotionai.billing.domain.model

import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class PaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    EXPIRED,
    CANCELED,
    DISPUTED,
    REFUNDED
}

class PaymentTransaction(
    val id: UUID,
    val partnerId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val cppSnapshot: BigDecimal,
    val expectedCredits: Long,
    val targetAccountId: UUID,
    val method: String?,
    val provider: String,
    val idempotencyKey: String?,
    val createdAt: Instant,
    val customerId: String? = null,
) {

    var providerCheckoutId: String? = null
        private set
    var providerCheckoutUrl: String? = null
        private set
    var status: PaymentStatus = PaymentStatus.PENDING
        private set
    var failureReason: String? = null
        private set
    var updatedAt: Instant = createdAt
        private set

    fun attachCheckout(providerCheckoutId: String?, providerCheckoutUrl: String?, updatedAt: Instant) {
        requirePendingForMutation()
        this.providerCheckoutId = providerCheckoutId
        this.providerCheckoutUrl = providerCheckoutUrl
        this.updatedAt = updatedAt
    }

    fun reopenForCheckout(providerCheckoutId: String?, providerCheckoutUrl: String?, updatedAt: Instant) {
        if (status != PaymentStatus.FAILED && status != PaymentStatus.EXPIRED && status != PaymentStatus.CANCELED) {
            throw InvalidPaymentStateException("Cannot retry checkout in status $status")
        }
        status = PaymentStatus.PENDING
        failureReason = null
        this.providerCheckoutId = providerCheckoutId
        this.providerCheckoutUrl = providerCheckoutUrl
        this.updatedAt = updatedAt
    }

    fun confirm(updatedAt: Instant) {
        if (!canConfirm()) {
            throw InvalidPaymentStateException("Cannot confirm payment in status $status")
        }
        status = PaymentStatus.CONFIRMED
        failureReason = null
        this.updatedAt = updatedAt
    }

    fun fail(reason: String?, updatedAt: Instant) {
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException("Cannot fail payment in status $status")
        }
        status = PaymentStatus.FAILED
        failureReason = reason
        this.updatedAt = updatedAt
    }

    fun expire(updatedAt: Instant) {
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException("Cannot expire payment in status $status")
        }
        status = PaymentStatus.EXPIRED
        this.updatedAt = updatedAt
    }

    fun cancel(updatedAt: Instant) {
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException("Cannot cancel payment in status $status")
        }
        status = PaymentStatus.CANCELED
        this.updatedAt = updatedAt
    }

    fun dispute(updatedAt: Instant) {
        if (status != PaymentStatus.CONFIRMED) {
            throw InvalidPaymentStateException("Cannot dispute payment in status $status")
        }
        status = PaymentStatus.DISPUTED
        this.updatedAt = updatedAt
    }

    fun refund(updatedAt: Instant) {
        if (status != PaymentStatus.CONFIRMED && status != PaymentStatus.DISPUTED) {
            throw InvalidPaymentStateException("Cannot refund payment in status $status")
        }
        status = PaymentStatus.REFUNDED
        this.updatedAt = updatedAt
    }

    fun canConfirm(): Boolean =
        status == PaymentStatus.PENDING || status == PaymentStatus.EXPIRED || status == PaymentStatus.DISPUTED

    fun isPending(): Boolean = status == PaymentStatus.PENDING

    fun isConfirmed(): Boolean = status == PaymentStatus.CONFIRMED

    private fun requirePendingForMutation() {
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException("Cannot attach checkout in status $status")
        }
    }

    internal fun restoreState(providerCheckoutId: String?, providerCheckoutUrl: String?, status: PaymentStatus,
                              failureReason: String?, updatedAt: Instant) {
        this.providerCheckoutId = providerCheckoutId
        this.providerCheckoutUrl = providerCheckoutUrl
        this.status = status
        this.failureReason = failureReason
        this.updatedAt = updatedAt
    }

    companion object {
        fun reconstitute(id: UUID, partnerId: UUID, amount: BigDecimal, currency: String, cppSnapshot: BigDecimal,
                         expectedCredits: Long, targetAccountId: UUID, method: String?, provider: String,
                         providerCheckoutId: String?, providerCheckoutUrl: String?, status: PaymentStatus,
                         failureReason: String?, idempotencyKey: String?, createdAt: Instant, updatedAt: Instant,
                         customerId: String? = null): PaymentTransaction {
            val transaction = PaymentTransaction(id, partnerId, amount, currency, cppSnapshot,
                expectedCredits, targetAccountId, method, provider, idempotencyKey, createdAt, customerId)
            transaction.restoreState(providerCheckoutId, providerCheckoutUrl, status, failureReason, updatedAt)
            return transaction
        }
    }
}
