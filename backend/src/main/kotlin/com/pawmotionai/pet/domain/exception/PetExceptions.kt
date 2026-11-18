package com.pawmotionai.pet.domain.exception

import java.util.UUID

class InvalidPetException(message: String) : RuntimeException(message)

class PetNotFoundException(petId: UUID) : RuntimeException("Pet not found: $petId")
