package com.pawmotionai.identity.adapter.out.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.pawmotionai.identity.application.port.out.TokenIssuer;
import com.pawmotionai.identity.domain.model.Owner;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long ttlSeconds;

    public JwtTokenIssuer(JwtEncoder jwtEncoder, @Value("${app.security.jwt.issuer}") String issuer,
                          @Value("${app.security.jwt.ttl-seconds}") long ttlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public IssuedToken issue(Owner owner) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(ttlSeconds))
            .subject(owner.getId().toString())
            .claim("email", owner.getEmail())
            .claim("displayName", owner.getDisplayName())
            .claim("role", owner.getRole())
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(token, "Bearer", ttlSeconds);
    }
}
