package com.transportlogistics.app.identity.infrastructure.config;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.application.service.IdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.application.ports.out.PasswordHasher;
import com.transportlogistics.app.identity.application.ports.out.RefreshTokenStore;
import com.transportlogistics.app.identity.infrastructure.security.JwtProperties;
import java.time.Clock;

@Configuration
class IdentityConfig {
    @Bean
    IdentityUseCase identityUseCase(IdentityRepository repository, PasswordHasher passwords,
                                    AccessTokenService accessTokens, RefreshTokenStore refreshTokens,
                                    JwtProperties properties, Clock clock) {
        return new IdentityService(repository, passwords, accessTokens, refreshTokens,
                properties.refreshTokenTtl(), clock);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
