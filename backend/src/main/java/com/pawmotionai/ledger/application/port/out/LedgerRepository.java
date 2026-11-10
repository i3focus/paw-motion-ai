package com.pawmotionai.ledger.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.ledger.domain.model.Wallet;

public interface LedgerRepository {

    Wallet saveWallet(Wallet wallet);

    Optional<Wallet> findWalletByPartnerId(UUID partnerId);

    Optional<Wallet> findWalletById(UUID walletId);

    Wallet findWalletForUpdate(UUID partnerId);

    List<LedgerAccount> findAccountsByWalletId(UUID walletId);

    List<LedgerAccount> findAccountsByWalletIdForUpdate(UUID walletId);

    LedgerAccount saveAccount(LedgerAccount account);

    LedgerEntry saveEntry(LedgerEntry entry);

    List<LedgerEntry> findEntriesByReference(LedgerReferenceType referenceType, UUID referenceId,
                                             LedgerEntryDirection direction);

    List<LedgerEntry> findEntriesByAccountIds(List<UUID> accountIds, int page, int size);

    long countEntriesByAccountIds(List<UUID> accountIds);
}
