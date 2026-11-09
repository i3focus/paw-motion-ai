package com.pawmotionai.partner.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

public interface PartnerCampaignRepository {

    PartnerCampaign save(PartnerCampaign campaign);

    Optional<PartnerCampaign> findById(UUID id);

    List<PartnerCampaign> findByPartnerId(UUID partnerId);

    List<PartnerCampaign> findByStatus(CampaignStatus status);
}
