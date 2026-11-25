package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.port.`in`.AuthenticatePartnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.application.port.out.PasswordHasher
import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException
import org.springframework.stereotype.Service

@Service
class AuthenticatePartnerService(
    private val ownerRepository: OwnerRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
) : AuthenticatePartnerUseCase {

    private val dummyHash: String = passwordHasher.hash(DUMMY_PASSWORD)

    override fun authenticate(command: LoginCommand): AuthResponseDto {
        val account = ownerRepository.findByEmailIgnoreCase(command.email.trim().lowercase())
        // Always compare against a hash (dummy when absent) so every rejection costs the same (C27).
        val passwordMatches = passwordHasher.matches(command.password, account?.passwordHash ?: dummyHash)
        val partner = account
            ?.takeIf { passwordMatches }
            ?.takeIf { it.isActive }
            ?.takeIf { it.isPartner }
            ?: throw InvalidCredentialsException()
        val token = tokenIssuer.issue(partner)
        return AuthResponseDto(token.accessToken, token.tokenType, token.expiresInSeconds, OwnerResponseDto.from(partner))
    }

    private companion object {
        const val DUMMY_PASSWORD = "paw-motion-dummy-password"
    }
}
