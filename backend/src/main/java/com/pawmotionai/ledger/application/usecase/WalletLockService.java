package com.pawmotionai.ledger.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.ledger.application.port.in.LockWalletUseCase;
import com.pawmotionai.ledger.application.port.out.LedgerRepository;

@Service
public class WalletLockService implements LockWalletUseCase {

    private final LedgerRepository repository;

    public WalletLockService(LedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void lock(UUID partnerId) {
        repository.findWalletForUpdate(partnerId);
    }
}
