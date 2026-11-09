package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.dto.CreateCampaignCommand;
import com.pawmotionai.partner.application.port.in.CreateCampaignUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class CreateCampaignService implements CreateCampaignUseCase {

    private final PartnerCampaignRepository repository;

    public CreateCampaignService(PartnerCampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CampaignResponseDto create(UUID partnerId, CreateCampaignCommand command) {
        PartnerCampaign campaign = PartnerCampaign.create(UUID.randomUUID(), partnerId,
            requireText(command.title(), "title"), requireText(command.description(), "description"),
            requireText(command.targetAudience(), "targetAudience"), requireText(command.callToAction(), "callToAction"),
            Instant.now());
        return CampaignResponseDto.from(repository.save(campaign));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
