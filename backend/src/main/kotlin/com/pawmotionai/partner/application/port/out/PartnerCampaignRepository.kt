package com.pawmotionai.partner.application.port.out

import com.pawmotionai.partner.domain.model.CampaignStatus
import com.pawmotionai.partner.domain.model.PartnerCampaign
import java.time.Instant
import java.util.UUID

interface PartnerCampaignRepository {

    fun save(campaign: PartnerCampaign): PartnerCampaign

    fun findById(id: UUID): PartnerCampaign?

    fun findByPartnerId(partnerId: UUID): List<PartnerCampaign>

    fun findByStatus(status: CampaignStatus): List<PartnerCampaign>

    fun findExpiringBefore(now: Instant, statuses: Collection<CampaignStatus>): List<PartnerCampaign>

    fun findByPartnerIdAndIdempotencyKey(partnerId: UUID, idempotencyKey: String): PartnerCampaign?
}
