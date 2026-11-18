package com.pawmotionai.ledger.application.dto

import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import com.pawmotionai.ledger.domain.model.WalletStatus
import java.time.Instant
import java.util.UUID

data class LedgerAccountResponseDto(
    val id: UUID,
    val walletId: UUID,
    val code: LedgerAccountCode,
    val status: LedgerAccountStatus,
    val balance: Long,
)

data class LedgerEntryResponseDto(
    val id: UUID,
    val accountId: UUID,
    val accountCode: LedgerAccountCode?,
    val direction: LedgerEntryDirection,
    val amount: Long,
    val balanceAfter: Long,
    val referenceType: LedgerReferenceType,
    val referenceId: UUID,
    val description: String?,
    val occurredAt: Instant,
)

data class LedgerEntryPage(
    val content: List<LedgerEntryResponseDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class WalletResponseDto(
    val walletId: UUID,
    val partnerId: UUID,
    val status: WalletStatus,
    val blockedReason: String?,
    val accounts: List<LedgerAccountResponseDto>,
    val totalBalance: Long,
)
