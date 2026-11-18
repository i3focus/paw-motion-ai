package com.pawmotionai.billing.application.usecase

import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException
import com.pawmotionai.billing.domain.model.PaymentStatus
import com.pawmotionai.billing.domain.model.PaymentTransaction
import com.pawmotionai.ledger.application.port.`in`.GetWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.CreditPosting
import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.DebitPosting
import com.pawmotionai.ledger.application.port.`in`.LockWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.PostLedgerEntryUseCase
import com.pawmotionai.ledger.application.port.`in`.SetWalletStatusUseCase
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import com.pawmotionai.ledger.domain.model.WalletStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PaymentLifecycleService(
    private val transactionRepository: PaymentTransactionRepository,
    private val postLedgerEntryUseCase: PostLedgerEntryUseCase,
    private val setWalletStatusUseCase: SetWalletStatusUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val lockWalletUseCase: LockWalletUseCase,
) {

    @Transactional
    fun confirm(partnerId: UUID, transactionId: UUID) {
        lockWalletUseCase.lock(partnerId)
        val transaction = load(transactionId)
        if (transaction.isConfirmed()) {
            return
        }
        val previousStatus = transaction.status
        val disputeResolved = previousStatus == PaymentStatus.DISPUTED
        if (previousStatus == PaymentStatus.PENDING || previousStatus == PaymentStatus.EXPIRED) {
            postLedgerEntryUseCase.credit(CreditPosting(transaction.partnerId, LedgerAccountCode.MAIN,
                transaction.expectedCredits, LedgerReferenceType.PAYMENT, transaction.id,
                "Credit purchase", transaction.id.toString()))
        }
        transaction.confirm(Instant.now())
        transactionRepository.save(transaction)
        if (disputeResolved) {
            unblockDispute(transaction.partnerId, transaction.id)
        }
    }

    @Transactional
    fun dispute(partnerId: UUID, transactionId: UUID) {
        lockWalletUseCase.lock(partnerId)
        val transaction = load(transactionId)
        transaction.dispute(Instant.now())
        transactionRepository.save(transaction)
        setWalletStatusUseCase.block(transaction.partnerId, "Payment dispute ${transaction.id}")
    }

    @Transactional
    fun refund(partnerId: UUID, transactionId: UUID, reason: String?) {
        lockWalletUseCase.lock(partnerId)
        val transaction = load(transactionId)
        val available = postLedgerEntryUseCase.availableBalance(transaction.partnerId)
        val reversal = minOf(transaction.expectedCredits, available)
        if (reversal > 0) {
            postLedgerEntryUseCase.debit(DebitPosting(transaction.partnerId, reversal,
                LedgerReferenceType.PAYMENT_REVERSAL, transaction.id, reason, transaction.id.toString()))
        }
        transaction.refund(Instant.now())
        transactionRepository.save(transaction)
        setWalletStatusUseCase.block(transaction.partnerId, reason)
    }

    private fun unblockDispute(partnerId: UUID, transactionId: UUID) {
        val wallet = getWalletUseCase.getByPartner(partnerId)
        val disputeReason = "Payment dispute $transactionId"
        if (wallet.status != WalletStatus.BLOCKED || wallet.blockedReason != disputeReason) {
            return
        }
        val remainingDispute = transactionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId)
            .firstOrNull { it.id != transactionId && it.status == PaymentStatus.DISPUTED }
        if (remainingDispute != null) {
            setWalletStatusUseCase.block(partnerId, "Payment dispute ${remainingDispute.id}")
        } else {
            setWalletStatusUseCase.unblock(partnerId)
        }
    }

    @Transactional
    fun expire(transactionId: UUID) {
        val transaction = load(transactionId)
        transaction.expire(Instant.now())
        transactionRepository.save(transaction)
    }

    @Transactional
    fun fail(transactionId: UUID, reason: String?) {
        val transaction = load(transactionId)
        transaction.fail(reason, Instant.now())
        transactionRepository.save(transaction)
    }

    private fun load(transactionId: UUID): PaymentTransaction =
        transactionRepository.findById(transactionId) ?: throw PaymentTransactionNotFoundException()
}
