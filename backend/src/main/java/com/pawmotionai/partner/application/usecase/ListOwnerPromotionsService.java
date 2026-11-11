package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.domain.model.Owner;
import com.pawmotionai.partner.application.dto.OwnerPromotionDto;
import com.pawmotionai.partner.application.port.in.ListOwnerPromotionsUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class ListOwnerPromotionsService implements ListOwnerPromotionsUseCase {

    private static final int MAX_PROMOTIONS = 12;

    private final PartnerCampaignRepository campaignRepository;
    private final OwnerRepository ownerRepository;

    public ListOwnerPromotionsService(PartnerCampaignRepository campaignRepository, OwnerRepository ownerRepository) {
        this.campaignRepository = campaignRepository;
        this.ownerRepository = ownerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerPromotionDto> listActivePromotions() {
        Instant now = Instant.now();
        List<PartnerCampaign> campaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE).stream()
            .filter(campaign -> campaign.getExpiresAt() == null || campaign.getExpiresAt().isAfter(now))
            .sorted(Comparator.comparing(PartnerCampaign::getCreatedAt).reversed())
            .limit(MAX_PROMOTIONS)
            .toList();

        Map<UUID, String> partnerNames = new HashMap<>();
        Map<UUID, String> partnerWebsites = new HashMap<>();
        for (UUID partnerId : campaigns.stream().map(PartnerCampaign::getPartnerId).distinct().toList()) {
            Owner owner = ownerRepository.findById(partnerId).orElse(null);
            partnerNames.put(partnerId, owner == null ? "Partner"
                : owner.getBusinessName() != null ? owner.getBusinessName() : owner.getDisplayName());
            partnerWebsites.put(partnerId, owner == null ? null : owner.getWebsite());
        }

        return campaigns.stream()
            .map(campaign -> OwnerPromotionDto.from(campaign,
                partnerNames.getOrDefault(campaign.getPartnerId(), "Partner"), partnerWebsites.get(campaign.getPartnerId())))
            .toList();
    }
}
