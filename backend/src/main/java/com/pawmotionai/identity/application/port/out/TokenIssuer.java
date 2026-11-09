package com.pawmotionai.identity.application.port.out;

import com.pawmotionai.identity.domain.model.Owner;

public interface TokenIssuer {

    IssuedToken issue(Owner owner);

    record IssuedToken(String accessToken, String tokenType, long expiresInSeconds) {
    }
}
