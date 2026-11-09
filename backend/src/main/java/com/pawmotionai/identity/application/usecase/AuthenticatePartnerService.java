package com.pawmotionai.identity.application.usecase;

import org.springframework.stereotype.Service;

import com.pawmotionai.identity.application.dto.AuthResponseDto;
import com.pawmotionai.identity.application.dto.LoginCommand;
import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.port.in.AuthenticatePartnerUseCase;
import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.application.port.out.PasswordHasher;
import com.pawmotionai.identity.application.port.out.TokenIssuer;
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException;
import com.pawmotionai.identity.domain.model.Owner;

@Service
public class AuthenticatePartnerService implements AuthenticatePartnerUseCase {

    private final OwnerRepository ownerRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticatePartnerService(OwnerRepository ownerRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.ownerRepository = ownerRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthResponseDto authenticate(LoginCommand command) {
        Owner partner = ownerRepository.findByEmailIgnoreCase(command.email().trim().toLowerCase())
            .filter(Owner::isActive)
            .filter(Owner::isPartner)
            .filter(candidate -> passwordHasher.matches(command.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        TokenIssuer.IssuedToken token = tokenIssuer.issue(partner);
        return new AuthResponseDto(token.accessToken(), token.tokenType(), token.expiresInSeconds(), OwnerResponseDto.from(partner));
    }
}
