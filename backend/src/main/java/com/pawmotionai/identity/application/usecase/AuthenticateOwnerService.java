package com.pawmotionai.identity.application.usecase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.LoginCommand;
import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.port.in.AuthenticateOwnerUseCase;
import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.application.port.out.PasswordHasher;
import com.pawmotionai.identity.application.port.out.TokenIssuer;
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException;
import com.pawmotionai.identity.domain.model.Owner;

@Service
public class AuthenticateOwnerService implements AuthenticateOwnerUseCase {

    private final OwnerRepository ownerRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final JdbcClient jdbc;
    private final Set<String> adminEmails;

    public AuthenticateOwnerService(OwnerRepository ownerRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
                                    JdbcClient jdbc, @Value("${app.admin.emails:}") String adminEmails) {
        this.ownerRepository = ownerRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.jdbc = jdbc;
        this.adminEmails = Arrays.stream(adminEmails.split(",")).map(String::trim).filter(email -> !email.isEmpty())
            .map(String::toLowerCase).collect(Collectors.toSet());
    }

    @Override
    public AuthResponseDto authenticate(LoginCommand command) {
        Owner owner = ownerRepository.findByEmailIgnoreCase(command.email().trim().toLowerCase())
            .filter(Owner::isActive)
            .filter(candidate -> !candidate.isPartner())
            .filter(candidate -> passwordHasher.matches(command.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        if (!"ADMIN".equals(owner.getRole()) && adminEmails.contains(owner.getEmail().toLowerCase())) {
            jdbc.sql("UPDATE owners SET role = 'ADMIN', updated_at = now() WHERE id = :id")
                .param("id", owner.getId()).update();
            owner = ownerRepository.findByEmailIgnoreCase(owner.getEmail()).orElse(owner);
        }

        TokenIssuer.IssuedToken token = tokenIssuer.issue(owner);
        return new AuthResponseDto(token.accessToken(), token.tokenType(), token.expiresInSeconds(), OwnerResponseDto.from(owner));
    }
}
