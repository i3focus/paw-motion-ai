package com.pawmotionai.ledger.domain.exception

import java.util.UUID

class InsufficientCreditsException(
    val requiredCredits: Long,
    val availableBalance: Long,
) : RuntimeException("Insufficient credits: required $requiredCredits, available $availableBalance")

class LedgerConflictException(message: String) : RuntimeException(message)

class WalletBlockedException(reason: String?) :
    RuntimeException(if (reason.isNullOrBlank()) "Wallet is blocked" else reason)

class WalletNotFoundException(partnerId: UUID) : RuntimeException("Wallet not found for partner $partnerId")
