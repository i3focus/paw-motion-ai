package com.pawmotionai.identity.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
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
import com.pawmotionai.ledger.application.port.in.EnsureWalletUseCase;

@Service
public class RegisterPartnerService implements RegisterPartnerUseCase {

    private final OwnerRepository ownerRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final EnsureWalletUseCase ensureWalletUseCase;
    private final JdbcTemplate jdbcTemplate;

    public RegisterPartnerService(OwnerRepository ownerRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
                                  EnsureWalletUseCase ensureWalletUseCase, JdbcTemplate jdbcTemplate) {
        this.ownerRepository = ownerRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.ensureWalletUseCase = ensureWalletUseCase;
        this.jdbcTemplate = jdbcTemplate;
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
            Instant.now());
        Owner saved = ownerRepository.save(partner);
        jdbcTemplate.update(
            "INSERT INTO partner_profiles (partner_id, business_name, website, description, created_at, updated_at) VALUES (?, ?, ?, ?, now(), now())",
            saved.getId(),
            command.businessName() == null || command.businessName().isBlank() ? saved.getDisplayName() : command.businessName().trim(),
            command.website() == null || command.website().isBlank() ? null : command.website().trim(),
            command.description() == null || command.description().isBlank() ? null : command.description().trim());
        ensureWalletUseCase.ensureWallet(saved.getId());
        TokenIssuer.IssuedToken token = tokenIssuer.issue(saved);
        return new AuthResponseDto(token.accessToken(), token.tokenType(), token.expiresInSeconds(), OwnerResponseDto.from(saved));
    }
}
