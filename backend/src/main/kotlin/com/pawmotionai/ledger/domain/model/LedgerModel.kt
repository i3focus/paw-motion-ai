package com.pawmotionai.ledger.domain.model

import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException
import java.time.Instant
import java.util.UUID

enum class LedgerAccountCode(val debitPriority: Int) {
    MAIN(1),
    BONUS(2),
    PARTNER(3)
}

enum class LedgerAccountStatus {
    OPEN,
    CLOSED
}

enum class LedgerEntryDirection {
    CREDIT,
    DEBIT
}

enum class LedgerReferenceType {
    PAYMENT,
    CAMPAIGN,
    CAMPAIGN_REFUND,
    PAYMENT_REVERSAL,
    ADJUSTMENT
}

enum class WalletStatus {
    ACTIVE,
    BLOCKED
}

class LedgerAccount(
    val id: UUID,
    val walletId: UUID,
    val code: LedgerAccountCode,
    val debitPriority: Int,
    var status: LedgerAccountStatus,
    var balance: Long,
    val createdAt: Instant,
    var updatedAt: Instant,
) {

    fun credit(amount: Long, occurredAt: Instant) {
        requirePositive(amount)
        requireOpen()
        balance += amount
        updatedAt = occurredAt
    }

    fun debit(amount: Long, occurredAt: Instant) {
        requirePositive(amount)
        requireOpen()
        if (amount > balance) {
            throw InsufficientCreditsException(amount, balance)
        }
        balance -= amount
        updatedAt = occurredAt
    }

    fun close(occurredAt: Instant) {
        status = LedgerAccountStatus.CLOSED
        updatedAt = occurredAt
    }

    private fun requirePositive(amount: Long) {
        require(amount > 0) { "Ledger amount must be positive" }
    }

    private fun requireOpen() {
        check(status == LedgerAccountStatus.OPEN) { "Ledger account $code is closed" }
    }
}

class LedgerEntry(
    val id: UUID,
    val accountId: UUID,
    val direction: LedgerEntryDirection,
    val amount: Long,
    val balanceAfter: Long,
    val referenceType: LedgerReferenceType,
    val referenceId: UUID,
    val description: String?,
    val correlationId: String?,
    val occurredAt: Instant,
)

class Wallet(
    val id: UUID,
    val partnerId: UUID,
    var status: WalletStatus,
    var blockedReason: String?,
    val createdAt: Instant,
    var updatedAt: Instant,
) {

    fun block(reason: String?, updatedAt: Instant) {
        status = WalletStatus.BLOCKED
        blockedReason = reason
        this.updatedAt = updatedAt
    }

    fun unblock(updatedAt: Instant) {
        status = WalletStatus.ACTIVE
        blockedReason = null
        this.updatedAt = updatedAt
    }

    fun isBlocked(): Boolean = status == WalletStatus.BLOCKED
}
