package com.pawmotionai.pet.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.ListPetsUseCase;
import com.pawmotionai.pet.application.port.out.PetRepository;

@Service
public class ListPetsService implements ListPetsUseCase {

    private final PetRepository petRepository;

    public ListPetsService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponseDto> listPets(UUID ownerId) {
        return petRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
            .map(PetResponseDto::from)
            .toList();
    }
}
