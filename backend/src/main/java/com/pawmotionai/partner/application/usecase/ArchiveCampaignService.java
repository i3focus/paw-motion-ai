package com.pawmotionai.partner.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.ledger.application.dto.LedgerAccountResponseDto;
import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.GetWalletUseCase;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.CreditPosting;
import com.pawmotionai.ledger.application.port.in.PostLedgerEntryUseCase;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerEntry;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;
import com.pawmotionai.partner.application.dto.CampaignResponseDto;
import com.pawmotionai.partner.application.port.in.ArchiveCampaignUseCase;
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository;
import com.pawmotionai.partner.domain.exception.CampaignAccessDeniedException;
import com.pawmotionai.partner.domain.exception.CampaignNotFoundException;
import com.pawmotionai.partner.domain.model.CampaignStatus;
import com.pawmotionai.partner.domain.model.PartnerCampaign;

@Service
public class ArchiveCampaignService implements ArchiveCampaignUseCase {

    private final PartnerCampaignRepository repository;
    private final PostLedgerEntryUseCase postLedgerEntryUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public ArchiveCampaignService(PartnerCampaignRepository repository, PostLedgerEntryUseCase postLedgerEntryUseCase,
                                  GetWalletUseCase getWalletUseCase) {
        this.repository = repository;
        this.postLedgerEntryUseCase = postLedgerEntryUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @Override
    @Transactional
    public CampaignResponseDto archive(UUID partnerId, UUID campaignId) {
        PartnerCampaign campaign = repository.findById(campaignId)
            .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.belongsTo(partnerId)) {
            throw new CampaignAccessDeniedException();
        }
        if (campaign.getStatus() == CampaignStatus.ARCHIVED) {
            return CampaignResponseDto.from(campaign, postLedgerEntryUseCase.availableBalance(partnerId));
        }
        Instant now = Instant.now();
        long refundAmount = proratedRefund(campaign, now);
        refundCredits(campaign, refundAmount);
        campaign.archive(refundAmount);
        PartnerCampaign saved = repository.save(campaign);
        return CampaignResponseDto.from(saved, postLedgerEntryUseCase.availableBalance(partnerId));
    }

    private long proratedRefund(PartnerCampaign campaign, Instant now) {
        if (campaign.getCreditsCharged() <= 0 || campaign.getExpiresAt() == null || !now.isBefore(campaign.getExpiresAt())) {
            return 0;
        }
        long totalMillis = Math.max(1, ChronoUnit.MILLIS.between(campaign.getCreatedAt(), campaign.getExpiresAt()));
        long remainingMillis = Math.max(0, ChronoUnit.MILLIS.between(now, campaign.getExpiresAt()));
        return campaign.getCreditsCharged() * remainingMillis / totalMillis;
    }

    private void refundCredits(PartnerCampaign campaign, long refundAmount) {
        if (refundAmount <= 0) {
            return;
        }
        List<LedgerEntry> debits = postLedgerEntryUseCase.findByReference(LedgerReferenceType.CAMPAIGN, campaign.getId(),
            LedgerEntryDirection.DEBIT);
        Map<UUID, Long> debitByAccount = debits.stream()
            .collect(Collectors.groupingBy(LedgerEntry::getAccountId, Collectors.summingLong(LedgerEntry::getAmount)));
        WalletResponseDto wallet = getWalletUseCase.getByPartner(campaign.getPartnerId());
        Map<UUID, LedgerAccountResponseDto> accountsById = wallet.accounts().stream()
            .collect(Collectors.toMap(LedgerAccountResponseDto::id, account -> account));

        long remaining = refundAmount;
        List<LedgerAccountResponseDto> reverseOrder = wallet.accounts().stream()
            .filter(account -> debitByAccount.containsKey(account.id()))
            .sorted(Comparator.comparingInt((LedgerAccountResponseDto account) -> account.code().getDebitPriority()).reversed())
            .toList();
        for (LedgerAccountResponseDto account : reverseOrder) {
            if (remaining <= 0) {
                break;
            }
            LedgerAccountCode code = LedgerAccountCode.valueOf(account.code().name());
            long originalDebit = debitByAccount.getOrDefault(account.id(), 0L);
            long amount = Math.min(originalDebit, remaining);
            if (amount <= 0) {
                continue;
            }
            postLedgerEntryUseCase.credit(new CreditPosting(campaign.getPartnerId(), code, amount,
                LedgerReferenceType.CAMPAIGN_REFUND, campaign.getId(), "Campaign archive refund",
                campaign.getId().toString()));
            remaining -= amount;
        }
    }
}
