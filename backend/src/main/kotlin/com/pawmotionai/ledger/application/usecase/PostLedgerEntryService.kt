package com.pawmotionai.ledger.application.usecase

import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.CreditPosting
import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.DebitPosting
import com.pawmotionai.ledger.application.port.`in`.PostLedgerEntryUseCase
import com.pawmotionai.ledger.application.port.out.LedgerRepository
import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException
import com.pawmotionai.ledger.domain.exception.LedgerConflictException
import com.pawmotionai.ledger.domain.exception.WalletBlockedException
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus
import com.pawmotionai.ledger.domain.model.LedgerEntry
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.OptimisticLockException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

@Service
class PostLedgerEntryService(
    private val repository: LedgerRepository,
    transactionManager: PlatformTransactionManager,
    private val meterRegistry: MeterRegistry,
) : PostLedgerEntryUseCase {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun credit(command: CreditPosting): List<LedgerEntry> {
        validateCredit(command)
        val entries = executeWithRetry { creditInTransaction(command) }
        meterRegistry.counter("ledger.credits", "direction", "credit", "reference", command.referenceType.name).increment(command.amount.toDouble())
        return entries
    }

    override fun debit(command: DebitPosting): List<LedgerEntry> {
        validateDebit(command)
        val entries = executeWithRetry { debitInTransaction(command) }
        meterRegistry.counter("ledger.credits", "direction", "debit", "reference", command.referenceType.name).increment(entries.sumOf { it.amount }.toDouble())
        return entries
    }

    override fun availableBalance(partnerId: UUID): Long =
        repository.findWalletByPartnerId(partnerId)
            ?.let { wallet ->
                repository.findAccountsByWalletId(wallet.id)
                    .filter { it.status == LedgerAccountStatus.OPEN }
                    .sumOf { it.balance }
            } ?: 0L

    override fun findByReference(referenceType: LedgerReferenceType, referenceId: UUID,
                                 direction: LedgerEntryDirection): List<LedgerEntry> =
        repository.findEntriesByReference(referenceType, referenceId, direction)

    private fun creditInTransaction(command: CreditPosting): List<LedgerEntry> =
        transactionTemplate.execute {
            val wallet = repository.findWalletByPartnerId(command.partnerId)
                ?: throw WalletNotFoundException(command.partnerId)
            val accounts = repository.findAccountsByWalletIdForUpdate(wallet.id)
            val account = accounts.firstOrNull { it.code == command.accountCode }
                ?: throw IllegalArgumentException("Ledger account not found: ${command.accountCode}")

            val existing = existingEntries(command.referenceType, command.referenceId,
                LedgerEntryDirection.CREDIT, account.id)
            if (existing.isNotEmpty()) {
                return@execute existing
            }

            val now = Instant.now()
            account.credit(command.amount, now)
            repository.saveAccount(account)
            val entry = repository.saveEntry(LedgerEntry(UUID.randomUUID(), account.id,
                LedgerEntryDirection.CREDIT, command.amount, account.balance, command.referenceType,
                command.referenceId, command.description, command.correlationId, now))
            listOf(entry)
        }!!

    private fun debitInTransaction(command: DebitPosting): List<LedgerEntry> =
        transactionTemplate.execute {
            val wallet = repository.findWalletForUpdate(command.partnerId)
            if (wallet.isBlocked() && command.referenceType != LedgerReferenceType.PAYMENT_REVERSAL) {
                throw WalletBlockedException(wallet.blockedReason)
            }
            val accounts = repository.findAccountsByWalletIdForUpdate(wallet.id)
                .sortedBy { it.debitPriority }

            val existing = repository.findEntriesByReference(command.referenceType, command.referenceId,
                LedgerEntryDirection.DEBIT)
            if (existing.isNotEmpty()) {
                return@execute existing
            }

            val available = accounts
                .filter { it.status == LedgerAccountStatus.OPEN }
                .sumOf { it.balance }
            if (available < command.amount) {
                throw InsufficientCreditsException(command.amount, available)
            }

            val now = Instant.now()
            var remaining = command.amount
            val entries = mutableListOf<LedgerEntry>()
            for (account in accounts) {
                if (remaining == 0L) {
                    break
                }
                if (account.status != LedgerAccountStatus.OPEN || account.balance == 0L) {
                    continue
                }
                val debitAmount = minOf(remaining, account.balance)
                account.debit(debitAmount, now)
                repository.saveAccount(account)
                entries.add(repository.saveEntry(LedgerEntry(UUID.randomUUID(), account.id,
                    LedgerEntryDirection.DEBIT, debitAmount, account.balance, command.referenceType,
                    command.referenceId, command.description, command.correlationId, now)))
                remaining -= debitAmount
            }
            if (remaining != 0L) {
                throw InsufficientCreditsException(command.amount, available)
            }
            entries
        }!!

    private fun executeWithRetry(action: () -> List<LedgerEntry>): List<LedgerEntry> {
        repeat(MAX_ATTEMPTS) {
            try {
                return action()
            } catch (_: OptimisticLockingFailureException) {
            } catch (_: OptimisticLockException) {
            }
        }
        throw LedgerConflictException("Ledger posting conflicted after $MAX_ATTEMPTS attempts")
    }

    private fun existingEntries(referenceType: LedgerReferenceType, referenceId: UUID,
                                direction: LedgerEntryDirection, accountId: UUID): List<LedgerEntry> =
        repository.findEntriesByReference(referenceType, referenceId, direction)
            .filter { it.accountId == accountId }

    private fun validateCredit(command: CreditPosting?) {
        if (command == null || command.partnerId == null || command.accountCode == null
            || command.referenceType == null || command.referenceId == null) {
            throw IllegalArgumentException("Credit posting is incomplete")
        }
        if (command.amount <= 0) {
            throw IllegalArgumentException("Credit amount must be positive")
        }
    }

    private fun validateDebit(command: DebitPosting?) {
        if (command == null || command.partnerId == null || command.referenceType == null
            || command.referenceId == null) {
            throw IllegalArgumentException("Debit posting is incomplete")
        }
        if (command.amount <= 0) {
            throw IllegalArgumentException("Debit amount must be positive")
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
