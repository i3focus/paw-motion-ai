package com.pawmotionai.identity.application.usecase

import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.port.`in`.GetCurrentOwnerUseCase
import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetCurrentOwnerService(private val ownerRepository: OwnerRepository) : GetCurrentOwnerUseCase {

    override fun getCurrentOwner(ownerId: UUID): OwnerResponseDto =
        ownerRepository.findById(ownerId)
            ?.let(OwnerResponseDto::from)
            ?: throw OwnerNotFoundException(ownerId)
}
