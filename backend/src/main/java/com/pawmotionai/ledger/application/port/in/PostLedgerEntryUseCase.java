package com.pawmotionai.ledger.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.CreditPosting;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.DebitPosting;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

public interface PostLedgerEntryUseCase {

    List<LedgerEntry> credit(CreditPosting command);

    List<LedgerEntry> debit(DebitPosting command);

    long availableBalance(UUID partnerId);

    List<LedgerEntry> findByReference(LedgerReferenceType referenceType, UUID referenceId, LedgerEntryDirection direction);
}
