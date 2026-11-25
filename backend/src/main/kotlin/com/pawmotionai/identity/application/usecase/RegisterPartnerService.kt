package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand
import com.pawmotionai.identity.application.port.`in`.RegisterPartnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.application.port.out.PasswordHasher
import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException
import com.pawmotionai.identity.domain.model.Owner
import com.pawmotionai.ledger.application.port.`in`.EnsureWalletUseCase
import com.pawmotionai.notification.SendWelcomeEmailUseCase
import com.pawmotionai.shared.UrlValidation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RegisterPartnerService(
    private val ownerRepository: OwnerRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
    private val ensureWalletUseCase: EnsureWalletUseCase,
    private val sendWelcomeEmail: SendWelcomeEmailUseCase,
    private val jdbcTemplate: JdbcTemplate,
) : RegisterPartnerUseCase {

    @Transactional
    override fun register(command: RegisterPartnerCommand): AuthResponseDto {
        val website = UrlValidation.requireHttpUrlOrNull("website", command.website)
        val normalizedEmail = command.email.trim().lowercase()
        if (ownerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw OwnerAlreadyExistsException(normalizedEmail)
        }

        val partner = Owner.registerPartner(
            UUID.randomUUID(), normalizedEmail, passwordHasher.hash(command.password), command.displayName.trim(),
            Instant.now())
        val saved = ownerRepository.save(partner)
        val businessName = command.businessName?.takeIf { it.isNotBlank() }?.trim() ?: saved.displayName
        jdbcTemplate.update(
            "INSERT INTO partner_profiles (partner_id, business_name, website, description, created_at, updated_at) VALUES (?, ?, ?, ?, now(), now())",
            saved.id,
            businessName,
            website,
            command.description?.takeIf { it.isNotBlank() }?.trim())
        ensureWalletUseCase.ensureWallet(saved.id)
        sendWelcomeEmail.sendPartnerWelcome(saved.email, saved.displayName, businessName)
        val token = tokenIssuer.issue(saved)
        return AuthResponseDto(token.accessToken, token.tokenType, token.expiresInSeconds, OwnerResponseDto.from(saved))
    }
}
