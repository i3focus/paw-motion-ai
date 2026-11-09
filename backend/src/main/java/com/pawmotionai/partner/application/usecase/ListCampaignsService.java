package com.pawmotionai.partner.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.port.in.ListCampaignsUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;

@Service
public class ListCampaignsService implements ListCampaignsUseCase {

    private final PartnerCampaignRepository repository;

    public ListCampaignsService(PartnerCampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponseDto> listByPartner(UUID partnerId) {
        return repository.findByPartnerId(partnerId).stream()
            .map(CampaignResponseDto::from)
            .toList();
    }
}
