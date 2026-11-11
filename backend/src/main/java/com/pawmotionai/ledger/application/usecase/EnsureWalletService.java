package com.pawmotionai.ledger.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.ledger.application.port.in.EnsureWalletUseCase;
import com.pawmotionai.ledger.application.port.out.LedgerRepository;
import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus;
import com.pawmotionai.ledger.domain.model.Wallet;
import com.pawmotionai.ledger.domain.model.WalletStatus;

@Service
public class EnsureWalletService implements EnsureWalletUseCase {

    private final LedgerRepository repository;

    public EnsureWalletService(LedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void ensureWallet(UUID partnerId) {
        Wallet wallet = repository.findWalletByPartnerId(partnerId)
            .orElseGet(() -> {
                Instant now = Instant.now();
                return repository.saveWallet(new Wallet(UUID.randomUUID(), partnerId, WalletStatus.ACTIVE, null, now, now));
            });
        ensureAccounts(wallet);
    }

    private void ensureAccounts(Wallet wallet) {
        Instant now = Instant.now();
        Set<LedgerAccountCode> existing = repository.findAccountsByWalletId(wallet.getId()).stream()
            .map(LedgerAccount::getCode)
            .collect(Collectors.toSet());
        List.of(LedgerAccountCode.MAIN, LedgerAccountCode.BONUS, LedgerAccountCode.PARTNER).stream()
            .filter(code -> !existing.contains(code))
            .forEach(code -> repository.saveAccount(new LedgerAccount(UUID.randomUUID(), wallet.getId(), code,
                code.getDebitPriority(), LedgerAccountStatus.OPEN, 0, now, now)));
    }
}
