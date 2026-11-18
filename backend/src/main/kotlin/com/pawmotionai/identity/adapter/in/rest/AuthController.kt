package com.pawmotionai.identity.adapter.`in`.rest

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.RegisterOwnerCommand
import com.pawmotionai.identity.application.port.`in`.AuthenticateOwnerUseCase
import com.pawmotionai.identity.application.port.`in`.RegisterOwnerUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerOwnerUseCase: RegisterOwnerUseCase,
    private val authenticateOwnerUseCase: AuthenticateOwnerUseCase,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterOwnerRequest): ResponseEntity<AuthResponseDto> {
        val response = registerOwnerUseCase.register(RegisterOwnerCommand(request.email, request.password, request.displayName))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponseDto> {
        val response = authenticateOwnerUseCase.authenticate(LoginCommand(request.email, request.password))
        return ResponseEntity.ok(response)
    }
}
