package com.pawmotionai.identity.application.port.`in`

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.OwnerResponseDto
import com.pawmotionai.identity.application.dto.RegisterOwnerCommand
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand
import com.pawmotionai.identity.application.dto.UpdateProfileCommand
import java.util.UUID

interface AuthenticateOwnerUseCase {
    fun authenticate(command: LoginCommand): AuthResponseDto
}

interface AuthenticatePartnerUseCase {
    fun authenticate(command: LoginCommand): AuthResponseDto
}

interface GetCurrentOwnerUseCase {
    fun getCurrentOwner(ownerId: UUID): OwnerResponseDto
}

interface RegisterOwnerUseCase {
    fun register(command: RegisterOwnerCommand): AuthResponseDto
}

interface RegisterPartnerUseCase {
    fun register(command: RegisterPartnerCommand): AuthResponseDto
}

interface UpdateProfileUseCase {
    fun update(accountId: UUID, command: UpdateProfileCommand): OwnerResponseDto
}
