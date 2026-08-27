package com.transportlogistics.app.notification.infrastructure.testing;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
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
    EmailNotificationSenderPort e2eEmailNotificationSender() {
        return new E2eDeterministicEmailSender();
    }
}
