package com.pawmotionai.ledger.application.port.`in`

import com.pawmotionai.ledger.application.dto.LedgerEntryPage
import com.pawmotionai.ledger.application.dto.WalletResponseDto
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerEntry
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import java.util.UUID

interface EnsureWalletUseCase {
    fun ensureWallet(partnerId: UUID)
}

interface GetWalletUseCase {
    fun getByPartner(partnerId: UUID): WalletResponseDto
}

interface ListWalletEntriesUseCase {
    fun listByPartner(partnerId: UUID, accountId: UUID?, page: Int, size: Int): LedgerEntryPage
}

interface LockWalletUseCase {
    fun lock(partnerId: UUID)
}

interface PostLedgerEntryUseCase {
    fun credit(command: LedgerPostingCommands.CreditPosting): List<LedgerEntry>
    fun debit(command: LedgerPostingCommands.DebitPosting): List<LedgerEntry>
    fun availableBalance(partnerId: UUID): Long
    fun findByReference(referenceType: LedgerReferenceType, referenceId: UUID,
                        direction: LedgerEntryDirection): List<LedgerEntry>
}

interface SetWalletStatusUseCase {
    fun block(partnerId: UUID, reason: String?): WalletResponseDto
    fun unblock(partnerId: UUID): WalletResponseDto
}

object LedgerPostingCommands {

    data class CreditPosting(
        val partnerId: UUID,
        val accountCode: LedgerAccountCode,
        val amount: Long,
        val referenceType: LedgerReferenceType,
        val referenceId: UUID,
        val description: String?,
        val correlationId: String?,
    )

    data class DebitPosting(
        val partnerId: UUID,
        val amount: Long,
        val referenceType: LedgerReferenceType,
        val referenceId: UUID,
        val description: String?,
        val correlationId: String?,
    )
}
