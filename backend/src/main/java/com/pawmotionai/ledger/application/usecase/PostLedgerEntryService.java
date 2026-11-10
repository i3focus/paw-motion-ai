package com.pawmotionai.ledger.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.OptimisticLockException;

import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.CreditPosting;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.DebitPosting;
import com.pawmotionai.ledger.application.port.in.PostLedgerEntryUseCase;
import com.pawmotionai.ledger.application.port.out.LedgerRepository;
import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException;
import com.pawmotionai.ledger.domain.exception.LedgerConflictException;
import com.pawmotionai.ledger.domain.exception.WalletBlockedException;
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException;
import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.ledger.domain.model.Wallet;

@Service
public class PostLedgerEntryService implements PostLedgerEntryUseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final LedgerRepository repository;
    private final TransactionTemplate transactionTemplate;

    public PostLedgerEntryService(LedgerRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<LedgerEntry> credit(CreditPosting command) {
        validateCredit(command);
        return executeWithRetry(() -> creditInTransaction(command));
    }

    @Override
    public List<LedgerEntry> debit(DebitPosting command) {
        validateDebit(command);
        return executeWithRetry(() -> debitInTransaction(command));
    }

    @Override
    public long availableBalance(UUID partnerId) {
        return repository.findWalletByPartnerId(partnerId)
            .map(wallet -> repository.findAccountsByWalletId(wallet.getId()).stream()
                .filter(account -> account.getStatus() == LedgerAccountStatus.OPEN)
                .mapToLong(LedgerAccount::getBalance)
                .sum())
            .orElse(0L);
    }

    @Override
    public List<LedgerEntry> findByReference(LedgerReferenceType referenceType, UUID referenceId, LedgerEntryDirection direction) {
        return repository.findEntriesByReference(referenceType, referenceId, direction);
    }

    private List<LedgerEntry> creditInTransaction(CreditPosting command) {
        return transactionTemplate.execute(status -> {
            Wallet wallet = repository.findWalletByPartnerId(command.partnerId())
                .orElseThrow(() -> new WalletNotFoundException(command.partnerId()));
            List<LedgerAccount> accounts = repository.findAccountsByWalletIdForUpdate(wallet.getId());
            LedgerAccount account = accounts.stream()
                .filter(candidate -> candidate.getCode() == command.accountCode())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ledger account not found: " + command.accountCode()));

            List<LedgerEntry> existing = existingEntries(command.referenceType(), command.referenceId(),
                LedgerEntryDirection.CREDIT, account.getId());
            if (!existing.isEmpty()) {
                return existing;
            }

            Instant now = Instant.now();
            account.credit(command.amount(), now);
            repository.saveAccount(account);
            LedgerEntry entry = repository.saveEntry(new LedgerEntry(UUID.randomUUID(), account.getId(),
                LedgerEntryDirection.CREDIT, command.amount(), account.getBalance(), command.referenceType(),
                command.referenceId(), command.description(), command.correlationId(), now));
            return List.of(entry);
        });
    }

    private List<LedgerEntry> debitInTransaction(DebitPosting command) {
        return transactionTemplate.execute(status -> {
            Wallet wallet = repository.findWalletForUpdate(command.partnerId());
            if (wallet.isBlocked()) {
                throw new WalletBlockedException(wallet.getBlockedReason());
            }
            List<LedgerAccount> accounts = repository.findAccountsByWalletIdForUpdate(wallet.getId()).stream()
                .sorted(Comparator.comparingInt(LedgerAccount::getDebitPriority))
                .toList();

            List<LedgerEntry> existing = repository.findEntriesByReference(command.referenceType(), command.referenceId(),
                LedgerEntryDirection.DEBIT);
            if (!existing.isEmpty()) {
                return existing;
            }

            long available = accounts.stream()
                .filter(account -> account.getStatus() == LedgerAccountStatus.OPEN)
                .mapToLong(LedgerAccount::getBalance)
                .sum();
            if (available < command.amount()) {
                throw new InsufficientCreditsException(command.amount(), available);
            }

            Instant now = Instant.now();
            long remaining = command.amount();
            List<LedgerEntry> entries = new ArrayList<>();
            for (LedgerAccount account : accounts) {
                if (remaining == 0) {
                    break;
                }
                if (account.getStatus() != LedgerAccountStatus.OPEN || account.getBalance() == 0) {
                    continue;
                }
                long debitAmount = Math.min(remaining, account.getBalance());
                account.debit(debitAmount, now);
                repository.saveAccount(account);
                entries.add(repository.saveEntry(new LedgerEntry(UUID.randomUUID(), account.getId(),
                    LedgerEntryDirection.DEBIT, debitAmount, account.getBalance(), command.referenceType(),
                    command.referenceId(), command.description(), command.correlationId(), now)));
                remaining -= debitAmount;
            }
            if (remaining != 0) {
                throw new InsufficientCreditsException(command.amount(), available);
            }
            return entries;
        });
    }

    private List<LedgerEntry> executeWithRetry(java.util.function.Supplier<List<LedgerEntry>> action) {
        OptimisticLockingFailureException lastOptimisticFailure = null;
        OptimisticLockException lastJpaOptimisticFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException exception) {
                lastOptimisticFailure = exception;
            } catch (OptimisticLockException exception) {
                lastJpaOptimisticFailure = exception;
            }
        }
        if (lastOptimisticFailure != null) {
            throw new LedgerConflictException("Ledger posting conflicted after " + MAX_ATTEMPTS + " attempts");
        }
        throw new LedgerConflictException("Ledger posting conflicted after " + MAX_ATTEMPTS + " attempts");
    }

    private List<LedgerEntry> existingEntries(LedgerReferenceType referenceType, UUID referenceId,
                                              LedgerEntryDirection direction, UUID accountId) {
        return repository.findEntriesByReference(referenceType, referenceId, direction).stream()
            .filter(entry -> entry.getAccountId().equals(accountId))
            .toList();
    }

    private void validateCredit(CreditPosting command) {
        if (command == null || command.partnerId() == null || command.accountCode() == null
            || command.referenceType() == null || command.referenceId() == null) {
            throw new IllegalArgumentException("Credit posting is incomplete");
        }
        if (command.amount() <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
    }

    private void validateDebit(DebitPosting command) {
        if (command == null || command.partnerId() == null || command.referenceType() == null
            || command.referenceId() == null) {
            throw new IllegalArgumentException("Debit posting is incomplete");
        }
        if (command.amount() <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
    }
}
