package com.pawmotionai.pet.adapter.`in`.rest

import com.pawmotionai.pet.application.dto.CreatePetCommand
import com.pawmotionai.pet.application.dto.PetResponseDto
import com.pawmotionai.pet.application.dto.UpdatePetCommand
import com.pawmotionai.pet.application.port.`in`.CreatePetUseCase
import com.pawmotionai.pet.application.port.`in`.GetPetUseCase
import com.pawmotionai.pet.application.port.`in`.ListPetsUseCase
import com.pawmotionai.pet.application.port.`in`.UpdatePetUseCase
import com.pawmotionai.shared.SecurityRoles
import com.pawmotionai.shared.ownerId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class CreatePetRequest(
    @field:NotBlank @field:Size(max = 80) val name: String,
    @field:NotBlank val species: String,
    @field:Size(max = 80) val breed: String?,
    @field:PastOrPresent val birthDate: LocalDate?,
    @field:NotBlank val size: String,
    @field:Size(max = 120) val activityPreference: String?,
)

@RestController
@Secured(SecurityRoles.OWNER)
@RequestMapping("/api/pets")
class PetController(
    private val createPetUseCase: CreatePetUseCase,
    private val listPetsUseCase: ListPetsUseCase,
    private val getPetUseCase: GetPetUseCase,
    private val updatePetUseCase: UpdatePetUseCase,
) {

    @GetMapping
    fun listPets(@AuthenticationPrincipal jwt: Jwt): List<PetResponseDto> = listPetsUseCase.listPets(jwt.ownerId)

    @PostMapping
    fun createPet(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreatePetRequest): ResponseEntity<PetResponseDto> {
        val response = createPetUseCase.createPet(CreatePetCommand(
            jwt.ownerId, request.name, request.species, request.breed, request.birthDate, request.size, request.activityPreference))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{petId}")
    fun getPet(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID): PetResponseDto =
        getPetUseCase.getPet(jwt.ownerId, petId)

    @PatchMapping("/{petId}")
    fun updatePet(@AuthenticationPrincipal jwt: Jwt, @PathVariable petId: UUID,
                  @Valid @RequestBody request: CreatePetRequest): PetResponseDto =
        updatePetUseCase.updatePet(UpdatePetCommand(
            jwt.ownerId, petId, request.name, request.species, request.breed, request.birthDate,
            request.size, request.activityPreference))
}
