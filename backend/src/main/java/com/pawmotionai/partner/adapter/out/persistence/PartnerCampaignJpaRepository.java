package com.pawmotionai.partner.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawmotionai.partner.domain.model.CampaignStatus;

public interface PartnerCampaignJpaRepository extends JpaRepository<PartnerCampaignJpaEntity, UUID> {

    List<PartnerCampaignJpaEntity> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    List<PartnerCampaignJpaEntity> findByStatus(CampaignStatus status);

    Optional<PartnerCampaignJpaEntity> findByIdAndPartnerId(UUID id, UUID partnerId);

    Optional<PartnerCampaignJpaEntity> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey);

    List<PartnerCampaignJpaEntity> findByStatusInAndExpiresAtBefore(Collection<CampaignStatus> statuses, Instant expiresAt);
}
