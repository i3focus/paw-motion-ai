package com.pawmotionai.partner.application.port.in;

import java.util.UUID;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.dto.CreateCampaignCommand;

public interface CreateCampaignUseCase {

    CampaignResponseDto create(UUID partnerId, CreateCampaignCommand command);
}
