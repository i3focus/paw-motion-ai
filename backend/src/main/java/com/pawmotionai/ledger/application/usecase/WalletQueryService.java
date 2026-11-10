package com.pawmotionai.ledger.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.ledger.application.dto.LedgerAccountResponseDto;
import com.pawmotionai.ledger.application.dto.LedgerEntryPage;
import com.pawmotionai.ledger.application.dto.LedgerEntryResponseDto;
import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.GetWalletUseCase;
import com.pawmotionai.ledger.application.port.in.ListWalletEntriesUseCase;
import com.pawmotionai.ledger.application.port.out.LedgerRepository;
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException;
import com.pawmotionai.ledger.domain.model.LedgerAccount;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.Wallet;

@Service
public class WalletQueryService implements GetWalletUseCase, ListWalletEntriesUseCase {

    private final LedgerRepository repository;

    public WalletQueryService(LedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getByPartner(UUID partnerId) {
        Wallet wallet = repository.findWalletByPartnerId(partnerId)
            .orElseThrow(() -> new WalletNotFoundException(partnerId));
        List<LedgerAccount> accounts = repository.findAccountsByWalletId(wallet.getId());
        return toWalletResponse(wallet, accounts);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerEntryPage listByPartner(UUID partnerId, UUID accountId, int page, int size) {
        Wallet wallet = repository.findWalletByPartnerId(partnerId)
            .orElseThrow(() -> new WalletNotFoundException(partnerId));
        List<LedgerAccount> accounts = repository.findAccountsByWalletId(wallet.getId());
        List<UUID> accountIds = accountId == null
            ? accounts.stream().map(LedgerAccount::getId).toList()
            : accounts.stream().filter(account -> account.getId().equals(accountId)).map(LedgerAccount::getId).toList();
        if (accountIds.isEmpty()) {
            return new LedgerEntryPage(List.of(), Math.max(page, 0), Math.max(size, 1), 0, 0);
        }
        Map<UUID, LedgerAccount> accountById = accounts.stream().collect(Collectors.toMap(LedgerAccount::getId, account -> account));
        List<LedgerEntryResponseDto> content = repository.findEntriesByAccountIds(accountIds, Math.max(page, 0), Math.max(size, 1))
            .stream().map(entry -> toEntryResponse(entry, accountById)).toList();
        long total = repository.countEntriesByAccountIds(accountIds);
        int safeSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new LedgerEntryPage(content, Math.max(page, 0), safeSize, total, totalPages);
    }

    private WalletResponseDto toWalletResponse(Wallet wallet, List<LedgerAccount> accounts) {
        List<LedgerAccountResponseDto> accountResponses = accounts.stream()
            .map(account -> new LedgerAccountResponseDto(account.getId(), account.getWalletId(), account.getCode(),
                account.getStatus(), account.getBalance()))
            .toList();
        long total = accounts.stream().filter(account -> account.getStatus() == com.pawmotionai.ledger.domain.model.LedgerAccountStatus.OPEN)
            .mapToLong(LedgerAccount::getBalance).sum();
        return new WalletResponseDto(wallet.getId(), wallet.getPartnerId(), wallet.getStatus(), wallet.getBlockedReason(),
            accountResponses, total);
    }

    private LedgerEntryResponseDto toEntryResponse(LedgerEntry entry, Map<UUID, LedgerAccount> accountById) {
        LedgerAccount account = accountById.get(entry.getAccountId());
        return new LedgerEntryResponseDto(entry.getId(), entry.getAccountId(), account == null ? null : account.getCode(),
            entry.getDirection(), entry.getAmount(), entry.getBalanceAfter(), entry.getReferenceType(), entry.getReferenceId(),
            entry.getDescription(), entry.getOccurredAt());
    }
}
