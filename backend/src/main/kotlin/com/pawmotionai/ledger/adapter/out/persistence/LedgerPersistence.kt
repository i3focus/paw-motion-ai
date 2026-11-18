package com.pawmotionai.ledger.adapter.out.persistence

import com.pawmotionai.ledger.application.port.out.LedgerRepository
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException
import com.pawmotionai.ledger.domain.model.LedgerAccount
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus
import com.pawmotionai.ledger.domain.model.LedgerEntry
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import com.pawmotionai.ledger.domain.model.Wallet
import com.pawmotionai.ledger.domain.model.WalletStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ledger_wallets")
class LedgerWalletJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "partner_id", nullable = false, unique = true)
    var partnerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WalletStatus,

    @Column(name = "blocked_reason")
    var blockedReason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    var version: Long? = null,
)

@Entity
@Table(name = "ledger_accounts")
class LedgerAccountJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "wallet_id", nullable = false)
    var walletId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var code: LedgerAccountCode,

    @Column(name = "debit_priority", nullable = false)
    var debitPriority: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LedgerAccountStatus,

    @Column(nullable = false)
    var balance: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    var version: Long? = null,
)

@Entity
@Table(name = "ledger_entries")
class LedgerEntryJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "account_id", nullable = false)
    var accountId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var direction: LedgerEntryDirection,

    @Column(nullable = false)
    var amount: Long,

    @Column(name = "balance_after", nullable = false)
    var balanceAfter: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    var referenceType: LedgerReferenceType,

    @Column(name = "reference_id", nullable = false)
    var referenceId: UUID,

    var description: String? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,
)

interface LedgerWalletJpaRepository : JpaRepository<LedgerWalletJpaEntity, UUID> {

    fun findByPartnerId(partnerId: UUID): LedgerWalletJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select w from LedgerWalletJpaEntity w where w.partnerId = :partnerId")
    fun findByPartnerIdForUpdate(@Param("partnerId") partnerId: UUID): LedgerWalletJpaEntity?
}

interface LedgerAccountJpaRepository : JpaRepository<LedgerAccountJpaEntity, UUID> {

    fun findByWalletIdOrderByDebitPriorityAsc(walletId: UUID): List<LedgerAccountJpaEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select a from LedgerAccountJpaEntity a where a.walletId = :walletId order by a.debitPriority asc")
    fun findByWalletIdForUpdateOrderByDebitPriorityAsc(@Param("walletId") walletId: UUID): List<LedgerAccountJpaEntity>
}

interface LedgerEntryJpaRepository : JpaRepository<LedgerEntryJpaEntity, UUID> {

    fun findByReferenceTypeAndReferenceIdAndDirection(
        referenceType: LedgerReferenceType, referenceId: UUID,
        direction: LedgerEntryDirection): List<LedgerEntryJpaEntity>

    fun findByAccountIdInOrderByOccurredAtDesc(accountIds: List<UUID>, pageable: Pageable): List<LedgerEntryJpaEntity>

    fun countByAccountIdIn(accountIds: List<UUID>): Long
}

@Repository
class LedgerPersistenceAdapter(
    private val walletRepository: LedgerWalletJpaRepository,
    private val accountRepository: LedgerAccountJpaRepository,
    private val entryRepository: LedgerEntryJpaRepository,
) : LedgerRepository {

    override fun saveWallet(wallet: Wallet): Wallet {
        val entity = walletRepository.findById(wallet.id)
            .orElseGet {
                LedgerWalletJpaEntity(wallet.id, wallet.partnerId, wallet.status,
                    wallet.blockedReason, wallet.createdAt, wallet.updatedAt)
            }
        entity.status = wallet.status
        entity.blockedReason = wallet.blockedReason
        entity.updatedAt = wallet.updatedAt
        return toDomain(walletRepository.save(entity))
    }

    override fun findWalletByPartnerId(partnerId: UUID): Wallet? =
        walletRepository.findByPartnerId(partnerId)?.let(::toDomain)

    override fun findWalletById(walletId: UUID): Wallet? =
        walletRepository.findById(walletId).map(::toDomain).orElse(null)

    override fun findWalletForUpdate(partnerId: UUID): Wallet =
        walletRepository.findByPartnerIdForUpdate(partnerId)
            ?.let(::toDomain)
            ?: throw WalletNotFoundException(partnerId)

    override fun findAccountsByWalletId(walletId: UUID): List<LedgerAccount> =
        accountRepository.findByWalletIdOrderByDebitPriorityAsc(walletId).map(::toDomain)

    override fun findAccountsByWalletIdForUpdate(walletId: UUID): List<LedgerAccount> =
        accountRepository.findByWalletIdForUpdateOrderByDebitPriorityAsc(walletId).map(::toDomain)

    override fun saveAccount(account: LedgerAccount): LedgerAccount {
        val entity = accountRepository.findById(account.id)
            .orElseGet {
                LedgerAccountJpaEntity(account.id, account.walletId, account.code,
                    account.debitPriority, account.status, account.balance, account.createdAt, account.updatedAt)
            }
        entity.balance = account.balance
        entity.status = account.status
        entity.updatedAt = account.updatedAt
        return toDomain(accountRepository.save(entity))
    }

    override fun saveEntry(entry: LedgerEntry): LedgerEntry =
        toDomain(entryRepository.save(LedgerEntryJpaEntity(entry.id, entry.accountId,
            entry.direction, entry.amount, entry.balanceAfter, entry.referenceType,
            entry.referenceId, entry.description, entry.correlationId, entry.occurredAt)))

    override fun findEntriesByReference(referenceType: LedgerReferenceType, referenceId: UUID,
                                        direction: LedgerEntryDirection): List<LedgerEntry> =
        entryRepository.findByReferenceTypeAndReferenceIdAndDirection(referenceType, referenceId, direction)
            .map(::toDomain)

    override fun findEntriesByAccountIds(accountIds: List<UUID>, page: Int, size: Int): List<LedgerEntry> {
        if (accountIds.isEmpty()) {
            return emptyList()
        }
        return entryRepository.findByAccountIdInOrderByOccurredAtDesc(accountIds, PageRequest.of(page, size))
            .map(::toDomain)
    }

    override fun countEntriesByAccountIds(accountIds: List<UUID>): Long =
        if (accountIds.isEmpty()) 0 else entryRepository.countByAccountIdIn(accountIds)

    private fun toDomain(entity: LedgerWalletJpaEntity): Wallet =
        Wallet(entity.id, entity.partnerId, entity.status, entity.blockedReason,
            entity.createdAt, entity.updatedAt)

    private fun toDomain(entity: LedgerAccountJpaEntity): LedgerAccount =
        LedgerAccount(entity.id, entity.walletId, entity.code, entity.debitPriority,
            entity.status, entity.balance, entity.createdAt, entity.updatedAt)

    private fun toDomain(entity: LedgerEntryJpaEntity): LedgerEntry =
        LedgerEntry(entity.id, entity.accountId, entity.direction, entity.amount,
            entity.balanceAfter, entity.referenceType, entity.referenceId, entity.description,
            entity.correlationId, entity.occurredAt)
}
