package com.pawmotionai.ledger.adapter.in.rest;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawmotionai.ledger.application.dto.LedgerEntryPage;
import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.EnsureWalletUseCase;
import com.pawmotionai.ledger.application.port.in.GetWalletUseCase;
import com.pawmotionai.ledger.application.port.in.ListWalletEntriesUseCase;

@RestController
@RequestMapping("/api/partner/wallet")
public class PartnerWalletController {

    private final GetWalletUseCase getWalletUseCase;
    private final ListWalletEntriesUseCase listWalletEntriesUseCase;
    private final EnsureWalletUseCase ensureWalletUseCase;

    public PartnerWalletController(GetWalletUseCase getWalletUseCase, ListWalletEntriesUseCase listWalletEntriesUseCase,
                                   EnsureWalletUseCase ensureWalletUseCase) {
        this.getWalletUseCase = getWalletUseCase;
        this.listWalletEntriesUseCase = listWalletEntriesUseCase;
        this.ensureWalletUseCase = ensureWalletUseCase;
    }

    @GetMapping
    public WalletResponseDto get(@AuthenticationPrincipal Jwt jwt) {
        UUID partnerId = UUID.fromString(jwt.getSubject());
        ensureWalletUseCase.ensureWallet(partnerId);
        return getWalletUseCase.getByPartner(partnerId);
    }

    @GetMapping("/accounts/{accountId}/entries")
    public LedgerEntryPage entries(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        UUID partnerId = UUID.fromString(jwt.getSubject());
        ensureWalletUseCase.ensureWallet(partnerId);
        return listWalletEntriesUseCase.listByPartner(partnerId, accountId, page, size);
    }
}
