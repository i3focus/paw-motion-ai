package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.dto.UpdateProfileCommand
import com.pawmotionai.identity.application.port.`in`.UpdateProfileUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException
import com.pawmotionai.shared.UrlValidation
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateProfileService(private val ownerRepository: OwnerRepository) : UpdateProfileUseCase {

    @Transactional
    override fun update(accountId: UUID, command: UpdateProfileCommand): OwnerResponseDto {
        val avatarUrl = command.avatarUrl?.let { UrlValidation.requireHttpUrlOrNull("avatarUrl", it) ?: "" }
        val owner = ownerRepository.findById(accountId) ?: throw OwnerNotFoundException(accountId)
        owner.updateProfile(command.displayName, command.bio, command.phone, avatarUrl)
        return OwnerResponseDto.from(ownerRepository.save(owner))
    }
}
