package com.pawmotionai.partner.application.port.in;

import java.util.UUID;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;

public interface ArchiveCampaignUseCase {

    CampaignResponseDto archive(UUID partnerId, UUID campaignId);
}
