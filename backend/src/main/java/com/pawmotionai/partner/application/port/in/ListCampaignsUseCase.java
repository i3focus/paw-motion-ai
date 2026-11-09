package com.pawmotionai.partner.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;

public interface ListCampaignsUseCase {

    List<CampaignResponseDto> listByPartner(UUID partnerId);
}
