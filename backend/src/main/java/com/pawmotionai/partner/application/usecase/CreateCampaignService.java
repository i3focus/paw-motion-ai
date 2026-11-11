package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.configuration.CampaignPricingProperties;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.DebitPosting;
import com.pawmotionai.ledger.application.port.in.PostLedgerEntryUseCase;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.dto.CreateCampaignCommand;
import com.pawmotionai.partner.application.port.in.CreateCampaignUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class CreateCampaignService implements CreateCampaignUseCase {

    private final PartnerCampaignRepository repository;
    private final CampaignPricingProperties pricingProperties;
    private final PostLedgerEntryUseCase postLedgerEntryUseCase;

    public CreateCampaignService(PartnerCampaignRepository repository, CampaignPricingProperties pricingProperties,
                                 PostLedgerEntryUseCase postLedgerEntryUseCase) {
        this.repository = repository;
        this.pricingProperties = pricingProperties;
        this.postLedgerEntryUseCase = postLedgerEntryUseCase;
    }

    @Override
    @Transactional
    public CampaignResponseDto create(UUID partnerId, CreateCampaignCommand command) {
        String idempotencyKey = command.idempotencyKey() == null || command.idempotencyKey().isBlank()
            ? null : command.idempotencyKey().trim();
        if (idempotencyKey != null) {
            var existing = repository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey);
            if (existing.isPresent()) {
                var existingCampaign = existing.get();
                if (!existingCampaign.getTitle().equals(normalizedText(command.title()))
                    || !existingCampaign.getDescription().equals(normalizedText(command.description()))
                    || !existingCampaign.getTargetAudience().equals(normalizedText(command.targetAudience()))
                    || !existingCampaign.getCallToAction().equals(normalizedText(command.callToAction()))) {
                    throw new IllegalArgumentException("Idempotency key was already used for a different campaign");
                }
                return CampaignResponseDto.from(existingCampaign, postLedgerEntryUseCase.availableBalance(partnerId));
            }
        }

        Instant now = Instant.now();
        long cost = pricingProperties.getCreationCostCredits();
        PartnerCampaign campaign = PartnerCampaign.createCharged(UUID.randomUUID(), partnerId,
            requireText(command.title(), "title"), requireText(command.description(), "description"),
            requireText(command.targetAudience(), "targetAudience"), requireText(command.callToAction(), "callToAction"),
            cost, now.plus(pricingProperties.getDurationDays(), ChronoUnit.DAYS), idempotencyKey, now);

        postLedgerEntryUseCase.debit(new DebitPosting(partnerId, cost, LedgerReferenceType.CAMPAIGN, campaign.getId(),
            "Campaign creation", command.idempotencyKey()));
        try {
            PartnerCampaign saved = repository.save(campaign);
            return CampaignResponseDto.from(saved, postLedgerEntryUseCase.availableBalance(partnerId));
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey != null) {
                return repository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)
                    .map(saved -> CampaignResponseDto.from(saved, postLedgerEntryUseCase.availableBalance(partnerId)))
                    .orElseThrow(() -> exception);
            }
            throw exception;
        }
    }

    private String normalizedText(String value) {
        return value == null ? null : value.trim();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
