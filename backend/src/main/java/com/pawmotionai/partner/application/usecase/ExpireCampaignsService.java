package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class ExpireCampaignsService {

    private static final Logger log = LoggerFactory.getLogger(ExpireCampaignsService.class);
    private static final List<CampaignStatus> EXPIRABLE = List.of(CampaignStatus.DRAFT, CampaignStatus.ACTIVE, CampaignStatus.PAUSED);

    private final PartnerCampaignRepository repository;

    public ExpireCampaignsService(PartnerCampaignRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${app.campaign.expiration-interval-ms:60000}", initialDelayString = "${app.campaign.expiration-initial-delay-ms:30000}")
    @Transactional
    public void expireCampaigns() {
        Instant now = Instant.now();
        for (PartnerCampaign campaign : repository.findExpiringBefore(now, EXPIRABLE)) {
            try {
                campaign.expire(now);
                repository.save(campaign);
            } catch (IllegalStateException exception) {
                log.debug("Campaign {} skipped for expiration: {}", campaign.getId(), exception.getMessage());
            }
        }
    }
}
