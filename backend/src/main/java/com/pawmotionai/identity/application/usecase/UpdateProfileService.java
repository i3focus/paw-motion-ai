package com.pawmotionai.identity.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.dto.UpdateProfileCommand;
import com.pawmotionai.identity.application.port.in.UpdateProfileUseCase;
import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException;
import com.pawmotionai.identity.domain.model.Owner;

@Service
public class UpdateProfileService implements UpdateProfileUseCase {

    private final OwnerRepository ownerRepository;

    public UpdateProfileService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    @Transactional
    public OwnerResponseDto update(UUID accountId, UpdateProfileCommand command) {
        Owner owner = ownerRepository.findById(accountId)
            .orElseThrow(() -> new OwnerNotFoundException(accountId));
        owner.updateProfile(command.displayName(), command.bio(), command.phone(), command.avatarUrl(),
            command.businessName(), command.website(), command.partnerDescription());
        return OwnerResponseDto.from(ownerRepository.save(owner));
    }
}
