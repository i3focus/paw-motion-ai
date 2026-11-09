package com.pawmotionai.pet.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.pet.application.dto.CreatePetCommand;
import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.CreatePetUseCase;
import com.pawmotionai.pet.application.port.in.GetPetUseCase;
import com.pawmotionai.pet.application.port.in.ListPetsUseCase;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final CreatePetUseCase createPetUseCase;
    private final ListPetsUseCase listPetsUseCase;
    private final GetPetUseCase getPetUseCase;

    public PetController(CreatePetUseCase createPetUseCase, ListPetsUseCase listPetsUseCase, GetPetUseCase getPetUseCase) {
        this.createPetUseCase = createPetUseCase;
        this.listPetsUseCase = listPetsUseCase;
        this.getPetUseCase = getPetUseCase;
    }

    @GetMapping
    public List<PetResponseDto> listPets(@AuthenticationPrincipal Jwt jwt) {
        return listPetsUseCase.listPets(ownerId(jwt));
    }

    @PostMapping
    public ResponseEntity<PetResponseDto> createPet(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePetRequest request) {
        PetResponseDto response = createPetUseCase.createPet(new CreatePetCommand(
            ownerId(jwt), request.name(), request.species(), request.breed(), request.birthDate(), request.size(), request.activityPreference()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{petId}")
    public PetResponseDto getPet(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId) {
        return getPetUseCase.getPet(ownerId(jwt), petId);
    }

    private UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
