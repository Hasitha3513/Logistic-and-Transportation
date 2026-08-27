package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.model.TokenClaims;
import com.transportlogistics.app.identity.domain.model.User;

public interface AccessTokenService {
    String issue(User user);

    TokenClaims verify(String token);

    long ttlSeconds();
}
