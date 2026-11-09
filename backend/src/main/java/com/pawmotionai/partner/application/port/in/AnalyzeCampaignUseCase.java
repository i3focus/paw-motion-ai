package com.pawmotionai.partner.application.port.in;

import java.util.UUID;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;

public interface AnalyzeCampaignUseCase {

    CampaignResponseDto analyze(UUID partnerId, UUID campaignId);
}
