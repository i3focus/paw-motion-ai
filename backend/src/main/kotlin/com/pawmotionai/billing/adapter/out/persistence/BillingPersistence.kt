package com.pawmotionai.billing.adapter.out.persistence

import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository
import com.pawmotionai.billing.application.port.out.PaymentWebhookEventRepository
import com.pawmotionai.billing.domain.model.PaymentStatus
import com.pawmotionai.billing.domain.model.PaymentTransaction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_transactions")
class PaymentTransactionJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "partner_id", nullable = false)
    var partnerId: UUID,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(name = "cpp_snapshot", nullable = false)
    var cppSnapshot: BigDecimal,

    @Column(name = "expected_credits", nullable = false)
    var expectedCredits: Long,

    @Column(name = "target_account_id", nullable = false)
    var targetAccountId: UUID,

    var method: String? = null,

    @Column(nullable = false)
    var provider: String,

    @Column(name = "provider_checkout_id")
    var providerCheckoutId: String? = null,

    @Column(name = "provider_checkout_url")
    var providerCheckoutUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "failure_reason")
    var failureReason: String? = null,

    @Column(name = "idempotency_key")
    var idempotencyKey: String? = null,

    @Column(name = "customer_id", length = 160)
    var customerId: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    var version: Long? = null,
)

interface PaymentTransactionJpaRepository : JpaRepository<PaymentTransactionJpaEntity, UUID> {

    fun findByPartnerIdOrderByCreatedAtDesc(partnerId: UUID): List<PaymentTransactionJpaEntity>

    fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String): PaymentTransactionJpaEntity?

    fun findByProviderAndProviderCheckoutId(provider: String, providerCheckoutId: String): PaymentTransactionJpaEntity?

    fun findByStatusAndCreatedAtBefore(status: PaymentStatus, createdBefore: Instant): List<PaymentTransactionJpaEntity>
}

data class PaymentWebhookEventId(
    var provider: String = "",
    var providerEventId: String = "",
) : Serializable

@Entity
@Table(name = "payment_webhook_events")
@IdClass(PaymentWebhookEventId::class)
class PaymentWebhookEventJpaEntity(

    @Id
    @Column(name = "provider", nullable = false)
    var provider: String,

    @Id
    @Column(name = "provider_event_id", nullable = false)
    var providerEventId: String,

    @Column(name = "payload_hash", nullable = false)
    var payloadHash: String,

    @Column(name = "received_at", nullable = false)
    var receivedAt: Instant,
)

interface PaymentWebhookEventJpaRepository : JpaRepository<PaymentWebhookEventJpaEntity, PaymentWebhookEventId> {

    fun existsByProviderAndProviderEventId(provider: String, providerEventId: String): Boolean
}

@Repository
class BillingPersistenceAdapter(
    private val transactionRepository: PaymentTransactionJpaRepository,
    private val webhookEventRepository: PaymentWebhookEventJpaRepository,
) : PaymentTransactionRepository, PaymentWebhookEventRepository {

    override fun save(transaction: PaymentTransaction): PaymentTransaction {
        val entity = transactionRepository.findById(transaction.id)
            .orElseGet {
                PaymentTransactionJpaEntity(transaction.id, transaction.partnerId,
                    transaction.amount, transaction.currency, transaction.cppSnapshot,
                    transaction.expectedCredits, transaction.targetAccountId, transaction.method,
                    transaction.provider, idempotencyKey = transaction.idempotencyKey,
                    createdAt = transaction.createdAt, updatedAt = transaction.updatedAt,
                    customerId = transaction.customerId)
            }
        entity.providerCheckoutId = transaction.providerCheckoutId
        entity.providerCheckoutUrl = transaction.providerCheckoutUrl
        entity.status = transaction.status
        entity.failureReason = transaction.failureReason
        entity.updatedAt = transaction.updatedAt
        return toDomain(transactionRepository.save(entity))
    }

    override fun findById(id: UUID): PaymentTransaction? =
        transactionRepository.findById(id).map(::toDomain).orElse(null)

    override fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String?): PaymentTransaction? =
        if (idempotencyKey == null) null
        else transactionRepository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)?.let(::toDomain)

    override fun findByProviderAndProviderCheckoutId(provider: String, providerCheckoutId: String): PaymentTransaction? =
        transactionRepository.findByProviderAndProviderCheckoutId(provider, providerCheckoutId)?.let(::toDomain)

    override fun findByPartnerIdOrderByCreatedAtDesc(partnerId: UUID): List<PaymentTransaction> =
        transactionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).map(::toDomain)

    override fun findPendingCreatedBefore(createdBefore: Instant): List<PaymentTransaction> =
        transactionRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, createdBefore)
            .map(::toDomain)

    override fun exists(provider: String, providerEventId: String): Boolean =
        webhookEventRepository.existsByProviderAndProviderEventId(provider, providerEventId)

    override fun save(provider: String, providerEventId: String, payloadHash: String, receivedAt: Instant) {
        webhookEventRepository.save(PaymentWebhookEventJpaEntity(provider, providerEventId, payloadHash, receivedAt))
    }

    private fun toDomain(entity: PaymentTransactionJpaEntity): PaymentTransaction =
        PaymentTransaction.reconstitute(entity.id, entity.partnerId, entity.amount, entity.currency,
            entity.cppSnapshot, entity.expectedCredits, entity.targetAccountId, entity.method,
            entity.provider, entity.providerCheckoutId, entity.providerCheckoutUrl, entity.status,
            entity.failureReason, entity.idempotencyKey, entity.createdAt, entity.updatedAt,
            entity.customerId)
}
