package com.pawmotionai.partner.adapter.out.persistence

import com.pawmotionai.partner.domain.model.CampaignEndReason
import com.pawmotionai.partner.domain.model.CampaignStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "partner_campaigns")
class PartnerCampaignJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "partner_id", nullable = false)
    var partnerId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var description: String,

    @Column(name = "target_audience", nullable = false)
    var targetAudience: String,

    @Column(name = "cta", nullable = false)
    var callToAction: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CampaignStatus,

    @Column(name = "ai_topics")
    var aiTopics: String? = null,

    @Column(name = "analyzed_at")
    var analyzedAt: Instant? = null,

    @Column(name = "credits_charged", nullable = false)
    var creditsCharged: Long,

    @Column(name = "credits_refunded", nullable = false)
    var creditsRefunded: Long,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant?,

    @Column(name = "idempotency_key")
    var idempotencyKey: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason")
    var endReason: CampaignEndReason? = null,

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant? = null,

    @Column(name = "ends_at", nullable = false)
    var endsAt: Instant? = null,

    @Column(name = "budget_credits", nullable = false)
    var budgetCredits: Long = 0,

    @Version
    var version: Long? = null,
) {

    fun setWindow(startsAt: Instant, endsAt: Instant?) {
        this.startsAt = startsAt
        this.endsAt = endsAt
    }

    fun update(title: String, description: String, targetAudience: String, callToAction: String,
               status: CampaignStatus, aiTopics: String?, analyzedAt: Instant?, creditsCharged: Long,
               creditsRefunded: Long, expiresAt: Instant?, idempotencyKey: String?, updatedAt: Instant) {
        this.title = title
        this.description = description
        this.targetAudience = targetAudience
        this.callToAction = callToAction
        this.status = status
        this.aiTopics = aiTopics
        this.analyzedAt = analyzedAt
        this.creditsCharged = creditsCharged
        this.creditsRefunded = creditsRefunded
        this.expiresAt = expiresAt
        this.idempotencyKey = idempotencyKey
        this.updatedAt = updatedAt
    }
}
