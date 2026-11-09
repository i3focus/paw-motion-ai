package com.pawmotionai.identity.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.port.in.GetCurrentOwnerUseCase;
import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException;

@Service
public class GetCurrentOwnerService implements GetCurrentOwnerUseCase {

    private final OwnerRepository ownerRepository;

    public GetCurrentOwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public OwnerResponseDto getCurrentOwner(UUID ownerId) {
        return ownerRepository.findById(ownerId)
            .map(OwnerResponseDto::from)
            .orElseThrow(() -> new OwnerNotFoundException(ownerId));
    }
}
