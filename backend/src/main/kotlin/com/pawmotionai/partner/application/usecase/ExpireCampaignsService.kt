package com.pawmotionai.partner.application.usecase

import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository
import com.pawmotionai.partner.domain.model.CampaignStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ExpireCampaignsService(private val repository: PartnerCampaignRepository) {

    @Scheduled(fixedDelayString = "\${app.campaign.expiration-interval-ms:60000}", initialDelayString = "\${app.campaign.expiration-initial-delay-ms:30000}")
    @Transactional
    fun expireCampaigns() {
        val now = Instant.now()
        for (campaign in repository.findExpiringBefore(now, EXPIRABLE)) {
            try {
                campaign.expire(now)
                repository.save(campaign)
            } catch (exception: IllegalStateException) {
                log.debug("Campaign {} skipped for expiration: {}", campaign.id, exception.message)
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ExpireCampaignsService::class.java)
        val EXPIRABLE = listOf(CampaignStatus.DRAFT, CampaignStatus.ACTIVE, CampaignStatus.PAUSED)
    }
}
