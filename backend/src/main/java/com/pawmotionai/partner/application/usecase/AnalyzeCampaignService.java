package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.port.in.AnalyzeCampaignUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.exception.CampaignAccessDeniedException;
import com.pawmotionai.partner.domain.exception.CampaignNotFoundException;
import com.pawmotionai.partner.domain.model.PartnerCampaign;
import com.pawmotionai.partner.domain.service.CampaignTopicAnalyzer;

@Service
public class AnalyzeCampaignService implements AnalyzeCampaignUseCase {

    private final PartnerCampaignRepository repository;
    private final CampaignTopicAnalyzer analyzer;

    public AnalyzeCampaignService(PartnerCampaignRepository repository, CampaignTopicAnalyzer analyzer) {
        this.repository = repository;
        this.analyzer = analyzer;
    }

    @Override
    @Transactional
    public CampaignResponseDto analyze(UUID partnerId, UUID campaignId) {
        PartnerCampaign campaign = repository.findById(campaignId)
            .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.belongsTo(partnerId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.applyAnalysis(analyzer.analyze(campaign), Instant.now());
        return CampaignResponseDto.from(repository.save(campaign));
    }
}
