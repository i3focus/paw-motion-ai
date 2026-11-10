package com.pawmotionai.ledger.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        if (repository.findWalletByPartnerId(partnerId).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        Wallet wallet = repository.saveWallet(new Wallet(UUID.randomUUID(), partnerId, WalletStatus.ACTIVE, null, now, now));
        List.of(LedgerAccountCode.MAIN, LedgerAccountCode.BONUS, LedgerAccountCode.PARTNER).forEach(code ->
            repository.saveAccount(new LedgerAccount(UUID.randomUUID(), wallet.getId(), code, code.getDebitPriority(),
                LedgerAccountStatus.OPEN, 0, now, now)));
    }
}
