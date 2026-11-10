package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

import com.pawmotionai.ledger.application.dto.LedgerEntryPage;

public interface ListWalletEntriesUseCase {

    LedgerEntryPage listByPartner(UUID partnerId, UUID accountId, int page, int size);
}
