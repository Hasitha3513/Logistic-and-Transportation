package com.transportlogistics.app.notification.infrastructure.config;

import com.transportlogistics.app.notification.application.ports.out.SmsNotificationSenderPort;
import com.transportlogistics.app.notification.infrastructure.adapters.out.sms.DeterministicSmsNotificationSenderAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.sms.DisabledSmsNotificationSenderAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(NotificationSmsProperties.class)
public class NotificationSmsConfiguration {
    @Bean
    SmsNotificationSenderPort smsNotificationSender(NotificationSmsProperties properties, Environment environment) {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
        properties.validate(productionProfile);
        return properties.isEnabled()
            ? new DeterministicSmsNotificationSenderAdapter()
            : new DisabledSmsNotificationSenderAdapter();
    }
}
