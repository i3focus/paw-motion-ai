package com.pawmotionai.partner.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.partner.application.dto.OwnerRecommendationDto;
import com.pawmotionai.partner.application.port.in.ListOwnerRecommendationsUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class ListOwnerRecommendationsService implements ListOwnerRecommendationsUseCase {

    private final PartnerCampaignRepository campaignRepository;
    private final OwnerRepository ownerRepository;

    public ListOwnerRecommendationsService(PartnerCampaignRepository campaignRepository, OwnerRepository ownerRepository) {
        this.campaignRepository = campaignRepository;
        this.ownerRepository = ownerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerRecommendationDto> listForOwners() {
        List<PartnerCampaign> campaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE).stream()
            .filter(campaign -> !campaign.topics().isEmpty())
            .toList();
        Map<UUID, String> partnerNames = campaigns.stream()
            .map(PartnerCampaign::getPartnerId)
            .distinct()
            .collect(Collectors.toMap(id -> id, id -> ownerRepository.findById(id)
                .map(owner -> owner.getBusinessName() != null ? owner.getBusinessName() : owner.getDisplayName())
                .orElse("Partner")));
        return campaigns.stream()
            .map(campaign -> OwnerRecommendationDto.from(campaign, partnerNames.getOrDefault(campaign.getPartnerId(), "Partner")))
            .toList();
    }
}
