package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

public final class LedgerPostingCommands {

    private LedgerPostingCommands() {
    }

    public record CreditPosting(UUID partnerId, LedgerAccountCode accountCode, long amount,
                                LedgerReferenceType referenceType, UUID referenceId, String description,
                                String correlationId) {
    }

    public record DebitPosting(UUID partnerId, long amount, LedgerReferenceType referenceType, UUID referenceId,
                               String description, String correlationId) {
    }
}
