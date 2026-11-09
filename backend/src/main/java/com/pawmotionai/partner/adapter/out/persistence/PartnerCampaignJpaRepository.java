package com.pawmotionai.partner.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawmotionai.partner.domain.model.CampaignStatus;

public interface PartnerCampaignJpaRepository extends JpaRepository<PartnerCampaignJpaEntity, UUID> {

    List<PartnerCampaignJpaEntity> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    List<PartnerCampaignJpaEntity> findByStatus(CampaignStatus status);

    Optional<PartnerCampaignJpaEntity> findByIdAndPartnerId(UUID id, UUID partnerId);
}
