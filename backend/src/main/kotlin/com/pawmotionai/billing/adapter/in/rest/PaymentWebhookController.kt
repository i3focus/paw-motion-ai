package com.pawmotionai.billing.adapter.`in`.rest

import com.pawmotionai.billing.application.port.`in`.ProcessPaymentWebhookUseCase
import com.pawmotionai.billing.application.port.out.WebhookRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhooks/payments")
class PaymentWebhookController(private val processPaymentWebhookUseCase: ProcessPaymentWebhookUseCase) {

    @PostMapping("/{provider}")
    fun receive(@PathVariable provider: String,
                @RequestHeader headers: HttpHeaders,
                @RequestBody body: ByteArray): ResponseEntity<Void> {
        processPaymentWebhookUseCase.process(WebhookRequest(provider, headers.toSingleValueMap(), body))
        return ResponseEntity.ok().build()
    }
}
