package com.pawmotionai.configuration;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pawmotionai.identity.application.port.out.OwnerRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ActiveOwnerFilter extends OncePerRequestFilter {

    private final OwnerRepository ownerRepository;

    public ActiveOwnerFilter(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                UUID ownerId = UUID.fromString(jwt.getSubject());
                boolean active = ownerRepository.findById(ownerId)
                    .map(owner -> owner.isActive())
                    .orElse(false);
                if (!active) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Owner account is disabled");
                    return;
                }
            } catch (IllegalArgumentException exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid owner subject");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
