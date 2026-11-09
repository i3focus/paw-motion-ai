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
import com.pawmotionai.identity.application.dto.RegisterOwnerCommand;
import com.pawmotionai.identity.application.port.in.AuthenticateOwnerUseCase;
import com.pawmotionai.identity.application.port.in.RegisterOwnerUseCase;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterOwnerUseCase registerOwnerUseCase;
    private final AuthenticateOwnerUseCase authenticateOwnerUseCase;

    public AuthController(RegisterOwnerUseCase registerOwnerUseCase, AuthenticateOwnerUseCase authenticateOwnerUseCase) {
        this.registerOwnerUseCase = registerOwnerUseCase;
        this.authenticateOwnerUseCase = authenticateOwnerUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterOwnerRequest request) {
        AuthResponseDto response = registerOwnerUseCase.register(new RegisterOwnerCommand(request.email(), request.password(), request.displayName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        AuthResponseDto response = authenticateOwnerUseCase.authenticate(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(response);
    }
}
