package com.pawmotionai.billing.domain.exception;

public class PaymentTransactionNotFoundException extends RuntimeException {

    public PaymentTransactionNotFoundException() {
        super("Payment transaction not found");
    }
}
