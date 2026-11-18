package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.dto.RegisterOwnerCommand
import com.pawmotionai.identity.application.port.`in`.RegisterOwnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.application.port.out.PasswordHasher
import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException
import com.pawmotionai.identity.domain.model.Owner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RegisterOwnerService(
    private val ownerRepository: OwnerRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
) : RegisterOwnerUseCase {

    @Transactional
    override fun register(command: RegisterOwnerCommand): AuthResponseDto {
        val normalizedEmail = command.email.trim().lowercase()
        if (ownerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw OwnerAlreadyExistsException(normalizedEmail)
        }

        val owner = Owner.register(
            UUID.randomUUID(),
            normalizedEmail,
            passwordHasher.hash(command.password),
            command.displayName.trim(),
            Instant.now())
        val saved = ownerRepository.save(owner)
        val token = tokenIssuer.issue(saved)
        return AuthResponseDto(token.accessToken, token.tokenType, token.expiresInSeconds, OwnerResponseDto.from(saved))
    }
}
