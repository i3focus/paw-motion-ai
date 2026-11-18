package com.pawmotionai.ledger.application.usecase

import com.pawmotionai.ledger.application.dto.LedgerAccountResponseDto
import com.pawmotionai.ledger.application.dto.LedgerEntryPage
import com.pawmotionai.ledger.application.dto.LedgerEntryResponseDto
import com.pawmotionai.ledger.application.dto.WalletResponseDto
import com.pawmotionai.ledger.application.port.`in`.EnsureWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.GetWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.ListWalletEntriesUseCase
import com.pawmotionai.ledger.application.port.`in`.LockWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.SetWalletStatusUseCase
import com.pawmotionai.ledger.application.port.out.LedgerRepository
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException
import com.pawmotionai.ledger.domain.model.LedgerAccount
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus
import com.pawmotionai.ledger.domain.model.LedgerEntry
import com.pawmotionai.ledger.domain.model.Wallet
import com.pawmotionai.ledger.domain.model.WalletStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil

@Service
class EnsureWalletService(private val repository: LedgerRepository) : EnsureWalletUseCase {

    @Transactional
    override fun ensureWallet(partnerId: UUID) {
        val wallet = repository.findWalletByPartnerId(partnerId)
            ?: run {
                val now = Instant.now()
                repository.saveWallet(Wallet(UUID.randomUUID(), partnerId, WalletStatus.ACTIVE, null, now, now))
            }
        ensureAccounts(wallet)
    }

    private fun ensureAccounts(wallet: Wallet) {
        val now = Instant.now()
        val existing = repository.findAccountsByWalletId(wallet.id).map { it.code }.toSet()
        listOf(LedgerAccountCode.MAIN, LedgerAccountCode.BONUS, LedgerAccountCode.PARTNER)
            .filter { it !in existing }
            .forEach { repository.saveAccount(LedgerAccount(UUID.randomUUID(), wallet.id, it,
                it.debitPriority, LedgerAccountStatus.OPEN, 0, now, now)) }
    }
}

@Service
class WalletLockService(private val repository: LedgerRepository) : LockWalletUseCase {

    @Transactional
    override fun lock(partnerId: UUID) {
        repository.findWalletForUpdate(partnerId)
    }
}

@Service
class WalletQueryService(private val repository: LedgerRepository) : GetWalletUseCase, ListWalletEntriesUseCase {

    @Transactional(readOnly = true)
    override fun getByPartner(partnerId: UUID): WalletResponseDto {
        val wallet = repository.findWalletByPartnerId(partnerId)
            ?: throw WalletNotFoundException(partnerId)
        val accounts = repository.findAccountsByWalletId(wallet.id)
        return toWalletResponse(wallet, accounts)
    }

    @Transactional(readOnly = true)
    override fun listByPartner(partnerId: UUID, accountId: UUID?, page: Int, size: Int): LedgerEntryPage {
        val wallet = repository.findWalletByPartnerId(partnerId)
            ?: throw WalletNotFoundException(partnerId)
        val accounts = repository.findAccountsByWalletId(wallet.id)
        val accountIds = if (accountId == null) {
            accounts.map { it.id }
        } else {
            accounts.filter { it.id == accountId }.map { it.id }
        }
        if (accountIds.isEmpty()) {
            return LedgerEntryPage(emptyList(), maxOf(page, 0), maxOf(size, 1), 0, 0)
        }
        val accountById = accounts.associateBy { it.id }
        val content = repository.findEntriesByAccountIds(accountIds, maxOf(page, 0), maxOf(size, 1))
            .map { toEntryResponse(it, accountById) }
        val total = repository.countEntriesByAccountIds(accountIds)
        val safeSize = maxOf(size, 1)
        val totalPages = ceil(total.toDouble() / safeSize).toInt()
        return LedgerEntryPage(content, maxOf(page, 0), safeSize, total, totalPages)
    }

    private fun toWalletResponse(wallet: Wallet, accounts: List<LedgerAccount>): WalletResponseDto {
        val accountResponses = accounts.map {
            LedgerAccountResponseDto(it.id, it.walletId, it.code, it.status, it.balance)
        }
        val total = accounts.filter { it.status == LedgerAccountStatus.OPEN }.sumOf { it.balance }
        return WalletResponseDto(wallet.id, wallet.partnerId, wallet.status, wallet.blockedReason,
            accountResponses, total)
    }

    private fun toEntryResponse(entry: LedgerEntry, accountById: Map<UUID, LedgerAccount>): LedgerEntryResponseDto {
        val account = accountById[entry.accountId]
        return LedgerEntryResponseDto(entry.id, entry.accountId, account?.code,
            entry.direction, entry.amount, entry.balanceAfter, entry.referenceType, entry.referenceId,
            entry.description, entry.occurredAt)
    }
}

@Service
class WalletStatusService(
    private val repository: LedgerRepository,
    private val walletQueryService: WalletQueryService,
) : SetWalletStatusUseCase {

    @Transactional
    override fun block(partnerId: UUID, reason: String?): WalletResponseDto {
        val wallet = repository.findWalletForUpdate(partnerId)
        wallet.block(reason, Instant.now())
        repository.saveWallet(wallet)
        return walletQueryService.getByPartner(partnerId)
    }

    @Transactional
    override fun unblock(partnerId: UUID): WalletResponseDto {
        val wallet = repository.findWalletForUpdate(partnerId)
        wallet.unblock(Instant.now())
        repository.saveWallet(wallet)
        return walletQueryService.getByPartner(partnerId)
    }
}
