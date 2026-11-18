package com.pawmotionai.identity.adapter.`in`.rest

import com.pawmotionai.identity.application.dto.AuthResponseDto
import com.pawmotionai.identity.application.dto.LoginCommand
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand
import com.pawmotionai.identity.application.port.`in`.AuthenticatePartnerUseCase
import com.pawmotionai.identity.application.port.`in`.RegisterPartnerUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/partner/auth")
class PartnerAuthController(
    private val registerPartnerUseCase: RegisterPartnerUseCase,
    private val authenticatePartnerUseCase: AuthenticatePartnerUseCase,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterPartnerRequest): ResponseEntity<AuthResponseDto> {
        val response = registerPartnerUseCase.register(RegisterPartnerCommand(request.email, request.password,
            request.displayName, request.businessName, request.website, request.description))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponseDto> =
        ResponseEntity.ok(authenticatePartnerUseCase.authenticate(LoginCommand(request.email, request.password)))
}
