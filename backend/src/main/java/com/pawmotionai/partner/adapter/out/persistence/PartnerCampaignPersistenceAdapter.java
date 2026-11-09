package com.pawmotionai.partner.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Repository
public class PartnerCampaignPersistenceAdapter implements PartnerCampaignRepository {

    private final PartnerCampaignJpaRepository repository;

    public PartnerCampaignPersistenceAdapter(PartnerCampaignJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PartnerCampaign save(PartnerCampaign campaign) {
        PartnerCampaignJpaEntity entity = repository.findById(campaign.getId())
            .orElseGet(() -> new PartnerCampaignJpaEntity(campaign.getId(), campaign.getPartnerId(), campaign.getTitle(),
                campaign.getDescription(), campaign.getTargetAudience(), campaign.getCallToAction(), campaign.getStatus(),
                null, null, campaign.getCreatedAt(), Instant.now()));
        entity.update(campaign.getTitle(), campaign.getDescription(), campaign.getTargetAudience(), campaign.getCallToAction(),
            campaign.getStatus(), joinTopics(campaign), campaign.getAnalyzedAt(), Instant.now());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<PartnerCampaign> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PartnerCampaign> findByPartnerId(UUID partnerId) {
        return repository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PartnerCampaign> findByStatus(CampaignStatus status) {
        return repository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    private String joinTopics(PartnerCampaign campaign) {
        return campaign.topics().isEmpty() ? null : String.join("\n", campaign.topics());
    }

    private PartnerCampaign toDomain(PartnerCampaignJpaEntity entity) {
        return PartnerCampaign.reconstitute(entity.getId(), entity.getPartnerId(), entity.getTitle(), entity.getDescription(),
            entity.getTargetAudience(), entity.getCallToAction(), entity.getStatus(), entity.getAiTopics(),
            entity.getAnalyzedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
