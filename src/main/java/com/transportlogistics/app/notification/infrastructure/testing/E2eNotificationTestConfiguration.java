package com.transportlogistics.app.notification.infrastructure.testing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("e2e")
public class E2eNotificationTestConfiguration {
    @Bean
    @Primary
    E2eAdjustableClock e2eAdjustableClock() {
        return new E2eAdjustableClock(Clock.systemUTC());
    }

    @Bean
    @Primary
    E2eDeterministicEmailSender e2eEmailNotificationSender() {
        return new E2eDeterministicEmailSender();
    }
}
