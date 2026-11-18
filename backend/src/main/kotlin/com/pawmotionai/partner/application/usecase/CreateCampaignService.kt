package com.pawmotionai.partner.application.usecase

import com.pawmotionai.configuration.CampaignPricingProperties
import com.pawmotionai.ledger.application.port.`in`.PostLedgerEntryUseCase
import com.pawmotionai.partner.application.dto.CampaignResponseDto
import com.pawmotionai.partner.application.dto.CreateCampaignCommand
import com.pawmotionai.partner.application.port.`in`.CreateCampaignUseCase
import com.pawmotionai.partner.application.port.out.PartnerCampaignRepository
import com.pawmotionai.partner.domain.model.PartnerCampaign
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class CreateCampaignService(
    private val repository: PartnerCampaignRepository,
    private val pricingProperties: CampaignPricingProperties,
    private val postLedgerEntryUseCase: PostLedgerEntryUseCase,
) : CreateCampaignUseCase {

    @Transactional
    override fun create(partnerId: UUID, command: CreateCampaignCommand): CampaignResponseDto {
        val idempotencyKey = command.idempotencyKey?.trim()?.ifEmpty { null }
        if (idempotencyKey != null) {
            val existingCampaign = repository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)
            if (existingCampaign != null) {
                if (existingCampaign.title != normalizedText(command.title)
                    || existingCampaign.description != normalizedText(command.description)
                    || existingCampaign.targetAudience != normalizedText(command.targetAudience)
                    || existingCampaign.callToAction != normalizedText(command.callToAction)) {
                    throw IllegalArgumentException("Idempotency key was already used for a different campaign")
                }
                return CampaignResponseDto.from(existingCampaign, postLedgerEntryUseCase.availableBalance(partnerId))
            }
        }

        // Drafts are free — credits are charged when the campaign is submitted for review (see AdsService#submit).
        val now = Instant.now()
        val campaign = PartnerCampaign.createCharged(UUID.randomUUID(), partnerId,
            requireText(command.title, "title"), requireText(command.description, "description"),
            requireText(command.targetAudience, "targetAudience"), requireText(command.callToAction, "callToAction"),
            0, now.plus(pricingProperties.durationDays.toLong(), ChronoUnit.DAYS), idempotencyKey, now)

        return try {
            val saved = repository.save(campaign)
            CampaignResponseDto.from(saved, postLedgerEntryUseCase.availableBalance(partnerId))
        } catch (exception: DataIntegrityViolationException) {
            if (idempotencyKey != null) {
                repository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)
                    ?.let { CampaignResponseDto.from(it, postLedgerEntryUseCase.availableBalance(partnerId)) }
                    ?: throw exception
            } else {
                throw exception
            }
        }
    }

    private fun normalizedText(value: String): String = value.trim()

    private fun requireText(value: String?, field: String): String {
        if (value.isNullOrBlank()) {
            throw IllegalArgumentException("$field is required")
        }
        return value
    }
}
