package com.pawmotionai.configuration;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.domain.model.Owner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ActiveAccountFilter extends OncePerRequestFilter {

    private static final List<String> OWNER_ONLY_PREFIXES = List.of(
        "/api/pets", "/api/activities", "/api/dashboard", "/api/insights");

    private final OwnerRepository ownerRepository;

    public ActiveAccountFilter(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Owner account;
            try {
                account = ownerRepository.findById(UUID.fromString(jwt.getSubject())).orElse(null);
            } catch (IllegalArgumentException exception) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid account subject");
                return;
            }
            if (account == null || !account.isActive()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is disabled");
                return;
            }
            String path = request.getRequestURI();
            String requiredRole = requiredRole(path);
            if (requiredRole != null && !requiredRole.equals(account.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient role for this endpoint");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String requiredRole(String path) {
        if (path.startsWith("/api/partner/") && !path.startsWith("/api/partner/auth")) {
            return "PARTNER";
        }
        if (OWNER_ONLY_PREFIXES.stream().anyMatch(path::startsWith)) {
            return "OWNER";
        }
        return null;
    }
}
