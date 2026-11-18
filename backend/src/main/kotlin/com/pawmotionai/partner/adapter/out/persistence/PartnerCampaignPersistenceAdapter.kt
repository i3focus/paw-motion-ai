package com.pawmotionai.partner.adapter.out.persistence

import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository
import com.pawmotionai.partner.domain.model.CampaignStatus
import com.pawmotionai.partner.domain.model.PartnerCampaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

interface PartnerCampaignJpaRepository : JpaRepository<PartnerCampaignJpaEntity, UUID> {

    fun findByPartnerIdOrderByCreatedAtDesc(partnerId: UUID): List<PartnerCampaignJpaEntity>

    fun findByStatus(status: CampaignStatus): List<PartnerCampaignJpaEntity>

    fun findByIdAndPartnerId(id: UUID, partnerId: UUID): PartnerCampaignJpaEntity?

    fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String): PartnerCampaignJpaEntity?

    fun findByStatusInAndExpiresAtBefore(statuses: Collection<CampaignStatus>, expiresAt: Instant): List<PartnerCampaignJpaEntity>
}

@Repository
class PartnerCampaignPersistenceAdapter(
    private val repository: PartnerCampaignJpaRepository,
) : PartnerCampaignRepository {

    override fun save(campaign: PartnerCampaign): PartnerCampaign {
        val entity = repository.findById(campaign.id)
            .orElseGet {
                PartnerCampaignJpaEntity(campaign.id, campaign.partnerId, campaign.title,
                    campaign.description, campaign.targetAudience, campaign.callToAction, campaign.status,
                    null, null, campaign.creditsCharged, campaign.creditsRefunded, campaign.expiresAt,
                    campaign.idempotencyKey, campaign.createdAt, Instant.now())
            }
        entity.update(campaign.title, campaign.description, campaign.targetAudience, campaign.callToAction,
            campaign.status, joinTopics(campaign), campaign.analyzedAt, campaign.creditsCharged,
            campaign.creditsRefunded, campaign.expiresAt, campaign.idempotencyKey, Instant.now())
        entity.endReason = campaign.endReason
        entity.setWindow(campaign.createdAt, campaign.expiresAt)
        entity.budgetCredits = campaign.creditsCharged
        return toDomain(repository.save(entity))
    }

    override fun findById(id: UUID): PartnerCampaign? = repository.findById(id).map(::toDomain).orElse(null)

    override fun findByPartnerId(partnerId: UUID): List<PartnerCampaign> =
        repository.findByPartnerIdOrderByCreatedAtDesc(partnerId).map(::toDomain)

    override fun findByStatus(status: CampaignStatus): List<PartnerCampaign> =
        repository.findByStatus(status).map(::toDomain)

    override fun findExpiringBefore(now: Instant, statuses: Collection<CampaignStatus>): List<PartnerCampaign> =
        repository.findByStatusInAndExpiresAtBefore(statuses, now).map(::toDomain)

    override fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String): PartnerCampaign? =
        repository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)?.let(::toDomain)

    private fun joinTopics(campaign: PartnerCampaign): String? =
        if (campaign.topics().isEmpty()) null else campaign.topics().joinToString("\n")

    private fun toDomain(entity: PartnerCampaignJpaEntity): PartnerCampaign {
        val campaign = PartnerCampaign.reconstitute(entity.id, entity.partnerId, entity.title, entity.description,
            entity.targetAudience, entity.callToAction, entity.status, entity.aiTopics,
            entity.analyzedAt, entity.creditsCharged, entity.creditsRefunded, entity.expiresAt,
            entity.idempotencyKey, entity.createdAt, entity.updatedAt)
        campaign.restoreEndReason(entity.endReason)
        return campaign
    }
}
