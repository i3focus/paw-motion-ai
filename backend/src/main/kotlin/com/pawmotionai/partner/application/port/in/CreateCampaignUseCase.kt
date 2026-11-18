package com.pawmotionai.partner.application.port.`in`

import com.pawmotionai.partner.application.dto.CampaignResponseDto
import com.pawmotionai.partner.application.dto.CreateCampaignCommand
import java.util.UUID

interface CreateCampaignUseCase {
    fun create(partnerId: UUID, command: CreateCampaignCommand): CampaignResponseDto
}
