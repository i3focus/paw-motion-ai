package com.pawmotionai.ledger.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.SetWalletStatusUseCase;
import com.pawmotionai.ledger.application.port.out.LedgerRepository;
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException;
import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.Wallet;

@Service
public class WalletStatusService implements SetWalletStatusUseCase {

    private final LedgerRepository repository;
    private final WalletQueryService walletQueryService;

    public WalletStatusService(LedgerRepository repository, WalletQueryService walletQueryService) {
        this.repository = repository;
        this.walletQueryService = walletQueryService;
    }

    @Override
    @Transactional
    public WalletResponseDto block(UUID partnerId, String reason) {
        Wallet wallet = repository.findWalletByPartnerId(partnerId)
            .orElseThrow(() -> new WalletNotFoundException(partnerId));
        wallet.block(reason, Instant.now());
        repository.saveWallet(wallet);
        return walletQueryService.getByPartner(partnerId);
    }

    @Override
    @Transactional
    public WalletResponseDto unblock(UUID partnerId) {
        Wallet wallet = repository.findWalletByPartnerId(partnerId)
            .orElseThrow(() -> new WalletNotFoundException(partnerId));
        wallet.unblock(Instant.now());
        repository.saveWallet(wallet);
        return walletQueryService.getByPartner(partnerId);
    }
}
