package com.pawmotionai.ledger.application.port.out

import com.pawmotionai.ledger.domain.model.LedgerAccount
import com.pawmotionai.ledger.domain.model.LedgerEntry
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import com.pawmotionai.ledger.domain.model.Wallet
import java.util.UUID

interface LedgerRepository {

    fun saveWallet(wallet: Wallet): Wallet

    fun findWalletByPartnerId(partnerId: UUID): Wallet?

    fun findWalletById(walletId: UUID): Wallet?

    fun findWalletForUpdate(partnerId: UUID): Wallet

    fun findAccountsByWalletId(walletId: UUID): List<LedgerAccount>

    fun findAccountsByWalletIdForUpdate(walletId: UUID): List<LedgerAccount>

    fun saveAccount(account: LedgerAccount): LedgerAccount

    fun saveEntry(entry: LedgerEntry): LedgerEntry

    fun findEntriesByReference(referenceType: LedgerReferenceType, referenceId: UUID,
                               direction: LedgerEntryDirection): List<LedgerEntry>

    fun findEntriesByAccountIds(accountIds: List<UUID>, page: Int, size: Int): List<LedgerEntry>

    fun countEntriesByAccountIds(accountIds: List<UUID>): Long
}
