package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.port.`in`.AuthenticateOwnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.application.port.out.PasswordHasher
import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException
import org.springframework.stereotype.Service

@Service
class AuthenticateOwnerService(
    private val ownerRepository: OwnerRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
) : AuthenticateOwnerUseCase {

    override fun authenticate(command: LoginCommand): AuthResponseDto {
        val owner = ownerRepository.findByEmailIgnoreCase(command.email.trim().lowercase())
            ?.takeIf { it.isActive }
            ?.takeIf { !it.isPartner }
            ?.takeIf { passwordHasher.matches(command.password, it.passwordHash) }
            ?: throw InvalidCredentialsException()

        val token = tokenIssuer.issue(owner)
        return AuthResponseDto(token.accessToken, token.tokenType, token.expiresInSeconds, OwnerResponseDto.from(owner))
    }
}
