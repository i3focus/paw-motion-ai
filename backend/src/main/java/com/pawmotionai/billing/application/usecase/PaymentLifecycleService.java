package com.pawmotionai.billing.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException;
import com.pawmotionai.billing.domain.model.PaymentStatus;
import com.pawmotionai.billing.domain.model.PaymentTransaction;
import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.GetWalletUseCase;
import com.pawmotionai.ledger.application.port.in.PostLedgerEntryUseCase;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.CreditPosting;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.DebitPosting;
import com.pawmotionai.ledger.application.port.in.SetWalletStatusUseCase;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.ledger.domain.model.WalletStatus;

@Service
public class PaymentLifecycleService {

    private final PaymentTransactionRepository transactionRepository;
    private final PostLedgerEntryUseCase postLedgerEntryUseCase;
    private final SetWalletStatusUseCase setWalletStatusUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public PaymentLifecycleService(PaymentTransactionRepository transactionRepository,
                                   PostLedgerEntryUseCase postLedgerEntryUseCase,
                                   SetWalletStatusUseCase setWalletStatusUseCase,
                                   GetWalletUseCase getWalletUseCase) {
        this.transactionRepository = transactionRepository;
        this.postLedgerEntryUseCase = postLedgerEntryUseCase;
        this.setWalletStatusUseCase = setWalletStatusUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @Transactional
    public void confirm(UUID transactionId) {
        PaymentTransaction transaction = load(transactionId);
        if (transaction.isConfirmed()) {
            return;
        }
        PaymentStatus previousStatus = transaction.getStatus();
        boolean disputeResolved = previousStatus == PaymentStatus.DISPUTED;
        if (previousStatus == PaymentStatus.PENDING || previousStatus == PaymentStatus.EXPIRED) {
            postLedgerEntryUseCase.credit(new CreditPosting(transaction.getPartnerId(), LedgerAccountCode.MAIN,
                transaction.getExpectedCredits(), LedgerReferenceType.PAYMENT, transaction.getId(),
                "Credit purchase", transaction.getId().toString()));
        }
        transaction.confirm(Instant.now());
        transactionRepository.save(transaction);
        if (disputeResolved) {
            unblockDispute(transaction.getPartnerId(), transaction.getId());
        }
    }

    @Transactional
    public void dispute(UUID transactionId) {
        PaymentTransaction transaction = load(transactionId);
        transaction.dispute(Instant.now());
        transactionRepository.save(transaction);
        setWalletStatusUseCase.block(transaction.getPartnerId(), "Payment dispute " + transaction.getId());
    }

    @Transactional
    public void refund(UUID transactionId, String reason) {
        PaymentTransaction transaction = load(transactionId);
        long available = postLedgerEntryUseCase.availableBalance(transaction.getPartnerId());
        long reversal = Math.min(transaction.getExpectedCredits(), available);
        if (reversal > 0) {
            postLedgerEntryUseCase.debit(new DebitPosting(transaction.getPartnerId(), reversal,
                LedgerReferenceType.PAYMENT_REVERSAL, transaction.getId(), reason, transaction.getId().toString()));
        }
        transaction.refund(Instant.now());
        transactionRepository.save(transaction);
        setWalletStatusUseCase.block(transaction.getPartnerId(), reason);
    }

    private void unblockDispute(UUID partnerId, UUID transactionId) {
        WalletResponseDto wallet = getWalletUseCase.getByPartner(partnerId);
        String disputeReason = "Payment dispute " + transactionId;
        if (wallet.status() != WalletStatus.BLOCKED || !disputeReason.equals(wallet.blockedReason())) {
            return;
        }
        var remainingDispute = transactionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
            .filter(candidate -> !candidate.getId().equals(transactionId) && candidate.getStatus() == PaymentStatus.DISPUTED)
            .findFirst();
        if (remainingDispute.isPresent()) {
            setWalletStatusUseCase.block(partnerId, "Payment dispute " + remainingDispute.get().getId());
        } else {
            setWalletStatusUseCase.unblock(partnerId);
        }
    }

    @Transactional
    public void expire(UUID transactionId) {
        PaymentTransaction transaction = load(transactionId);
        transaction.expire(Instant.now());
        transactionRepository.save(transaction);
    }

    @Transactional
    public void fail(UUID transactionId, String reason) {
        PaymentTransaction transaction = load(transactionId);
        transaction.fail(reason, Instant.now());
        transactionRepository.save(transaction);
    }

    private PaymentTransaction load(UUID transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(PaymentTransactionNotFoundException::new);
    }
}
