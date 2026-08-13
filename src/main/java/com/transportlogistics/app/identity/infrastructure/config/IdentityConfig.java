package com.transportlogistics.app.identity.infrastructure.config;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.application.service.IdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityConfig {
    @Bean
    IdentityUseCase identityUseCase(IdentityRepository r) {
        return new IdentityService(r);
    }
}