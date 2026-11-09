package com.pawmotionai.identity.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.LoginCommand;
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand;
import com.pawmotionai.identity.application.port.in.AuthenticatePartnerUseCase;
import com.pawmotionai.identity.application.port.in.RegisterPartnerUseCase;

@RestController
@RequestMapping("/api/partner/auth")
public class PartnerAuthController {

    private final RegisterPartnerUseCase registerPartnerUseCase;
    private final AuthenticatePartnerUseCase authenticatePartnerUseCase;

    public PartnerAuthController(RegisterPartnerUseCase registerPartnerUseCase,
                                 AuthenticatePartnerUseCase authenticatePartnerUseCase) {
        this.registerPartnerUseCase = registerPartnerUseCase;
        this.authenticatePartnerUseCase = authenticatePartnerUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterPartnerRequest request) {
        AuthResponseDto response = registerPartnerUseCase.register(new RegisterPartnerCommand(request.email(), request.password(),
            request.displayName(), request.businessName(), request.website(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticatePartnerUseCase.authenticate(new LoginCommand(request.email(), request.password())));
    }
}
