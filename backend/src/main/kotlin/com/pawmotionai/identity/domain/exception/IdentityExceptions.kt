package com.pawmotionai.identity.domain.exception

import java.util.UUID

class InvalidCredentialsException : RuntimeException("Invalid credentials")

class OwnerAlreadyExistsException(email: String) : RuntimeException("Owner already exists for email: $email")

class OwnerNotFoundException(ownerId: UUID) : RuntimeException("Owner not found: $ownerId")
