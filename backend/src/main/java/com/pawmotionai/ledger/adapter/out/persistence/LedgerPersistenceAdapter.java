package com.pawmotionai.ledger.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.pawmotionai.ledger.application.port.out.LedgerRepository;
import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.ledger.domain.model.Wallet;
import com.pawmotionai.ledger.domain.model.WalletStatus;

@Repository
public class LedgerPersistenceAdapter implements LedgerRepository {

    private final LedgerWalletJpaRepository walletRepository;
    private final LedgerAccountJpaRepository accountRepository;
    private final LedgerEntryJpaRepository entryRepository;

    public LedgerPersistenceAdapter(LedgerWalletJpaRepository walletRepository,
                                    LedgerAccountJpaRepository accountRepository,
                                    LedgerEntryJpaRepository entryRepository) {
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Override
    public Wallet saveWallet(Wallet wallet) {
        LedgerWalletJpaEntity entity = walletRepository.findById(wallet.getId())
            .orElseGet(() -> new LedgerWalletJpaEntity(wallet.getId(), wallet.getPartnerId(), wallet.getStatus(),
                wallet.getBlockedReason(), wallet.getCreatedAt(), wallet.getUpdatedAt()));
        entity.setStatus(wallet.getStatus());
        entity.setBlockedReason(wallet.getBlockedReason());
        entity.setUpdatedAt(wallet.getUpdatedAt());
        return toDomain(walletRepository.save(entity));
    }

    @Override
    public Optional<Wallet> findWalletByPartnerId(UUID partnerId) {
        return walletRepository.findByPartnerId(partnerId).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findWalletById(UUID walletId) {
        return walletRepository.findById(walletId).map(this::toDomain);
    }

    @Override
    public Wallet findWalletForUpdate(UUID partnerId) {
        return walletRepository.findByPartnerIdForUpdate(partnerId)
            .map(this::toDomain)
            .orElseThrow(() -> new IllegalStateException("Wallet not found for partner " + partnerId));
    }

    @Override
    public List<LedgerAccount> findAccountsByWalletId(UUID walletId) {
        return accountRepository.findByWalletIdOrderByDebitPriorityAsc(walletId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<LedgerAccount> findAccountsByWalletIdForUpdate(UUID walletId) {
        return accountRepository.findByWalletIdForUpdateOrderByDebitPriorityAsc(walletId).stream().map(this::toDomain).toList();
    }

    @Override
    public LedgerAccount saveAccount(LedgerAccount account) {
        LedgerAccountJpaEntity entity = accountRepository.findById(account.getId())
            .orElseGet(() -> new LedgerAccountJpaEntity(account.getId(), account.getWalletId(), account.getCode(),
                account.getDebitPriority(), account.getStatus(), account.getBalance(), account.getCreatedAt(),
                account.getUpdatedAt()));
        entity.setBalance(account.getBalance());
        entity.setStatus(account.getStatus());
        entity.setUpdatedAt(account.getUpdatedAt());
        return toDomain(accountRepository.save(entity));
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        return toDomain(entryRepository.save(new LedgerEntryJpaEntity(entry.getId(), entry.getAccountId(),
            entry.getDirection(), entry.getAmount(), entry.getBalanceAfter(), entry.getReferenceType(),
            entry.getReferenceId(), entry.getDescription(), entry.getCorrelationId(), entry.getOccurredAt())));
    }

    @Override
    public List<LedgerEntry> findEntriesByReference(LedgerReferenceType referenceType, UUID referenceId,
                                                    LedgerEntryDirection direction) {
        return entryRepository.findByReferenceTypeAndReferenceIdAndDirection(referenceType, referenceId, direction)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LedgerEntry> findEntriesByAccountIds(List<UUID> accountIds, int page, int size) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return entryRepository.findByAccountIdInOrderByOccurredAtDesc(accountIds, PageRequest.of(page, size))
            .stream().map(this::toDomain).toList();
    }

    @Override
    public long countEntriesByAccountIds(List<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return 0;
        }
        return entryRepository.countByAccountIdIn(accountIds);
    }

    private Wallet toDomain(LedgerWalletJpaEntity entity) {
        return new Wallet(entity.getId(), entity.getPartnerId(), entity.getStatus(), entity.getBlockedReason(),
            entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private LedgerAccount toDomain(LedgerAccountJpaEntity entity) {
        return new LedgerAccount(entity.getId(), entity.getWalletId(), entity.getCode(), entity.getDebitPriority(),
            entity.getStatus(), entity.getBalance(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return new LedgerEntry(entity.getId(), entity.getAccountId(), entity.getDirection(), entity.getAmount(),
            entity.getBalanceAfter(), entity.getReferenceType(), entity.getReferenceId(), entity.getDescription(),
            entity.getCorrelationId(), entity.getOccurredAt());
    }
}
