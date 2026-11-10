package com.pawmotionai.ledger.domain.model;

public enum LedgerAccountCode {
    MAIN(1),
    BONUS(2),
    PARTNER(3);

    private final int debitPriority;

    LedgerAccountCode(int debitPriority) {
        this.debitPriority = debitPriority;
    }

    public int getDebitPriority() {
        return debitPriority;
    }
}
