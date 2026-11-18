package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.port.`in`.AuthenticateOwnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.application.port.out.PasswordHasher
import com.pawmotionai.identity.application.port.out.TokenIssuer
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException
import com.pawmotionai.identity.domain.model.Owner
import com.pawmotionai.identity.domain.model.OwnerRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service

@Service
class AuthenticateOwnerService(
    private val ownerRepository: OwnerRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenIssuer: TokenIssuer,
    private val jdbc: JdbcClient,
    @param:Value("\${app.admin.emails:}") adminEmails: String,
) : AuthenticateOwnerUseCase {

    private val adminEmails: Set<String> = adminEmails.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.lowercase() }
        .toSet()

    override fun authenticate(command: LoginCommand): AuthResponseDto {
        var owner = ownerRepository.findByEmailIgnoreCase(command.email.trim().lowercase())
            ?.takeIf { it.isActive }
            ?.takeIf { !it.isPartner }
            ?.takeIf { passwordHasher.matches(command.password, it.passwordHash) }
            ?: throw InvalidCredentialsException()

        if (owner.role != OwnerRole.ADMIN && adminEmails.contains(owner.email.lowercase())) {
            jdbc.sql("UPDATE owners SET role = 'ADMIN', updated_at = now() WHERE id = :id")
                .param("id", owner.id).update()
            owner = ownerRepository.findByEmailIgnoreCase(owner.email) ?: owner
        }

        val token = tokenIssuer.issue(owner)
        return AuthResponseDto(token.accessToken, token.tokenType, token.expiresInSeconds, OwnerResponseDto.from(owner))
    }
}
