package com.pawmotionai.identity.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.dto.RegisterPartnerCommand;
import com.pawmotionai.identity.application.port.in.RegisterPartnerUseCase;
import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.application.port.out.PasswordHasher;
import com.pawmotionai.identity.application.port.out.TokenIssuer;
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException;
import com.pawmotionai.identity.domain.model.Owner;

@Service
public class RegisterPartnerService implements RegisterPartnerUseCase {

    private final OwnerRepository ownerRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public RegisterPartnerService(OwnerRepository ownerRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.ownerRepository = ownerRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterPartnerCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();
        if (ownerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new OwnerAlreadyExistsException(normalizedEmail);
        }

        Owner partner = Owner.registerPartner(
            UUID.randomUUID(), normalizedEmail, passwordHasher.hash(command.password()), command.displayName().trim(),
            command.businessName(), command.website(), command.description(), Instant.now());
        Owner saved = ownerRepository.save(partner);
        TokenIssuer.IssuedToken token = tokenIssuer.issue(saved);
        return new AuthResponseDto(token.accessToken(), token.tokenType(), token.expiresInSeconds(), OwnerResponseDto.from(saved));
    }
}
