package com.pawmotionai.billing.domain.exception

class InvalidPaymentStateException(message: String) : RuntimeException(message)

class PaymentGatewayException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class PaymentTransactionNotFoundException : RuntimeException("Payment transaction not found")

class WebhookVerificationException(message: String) : RuntimeException(message)
