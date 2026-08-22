package com.transportlogistics.app.notification.infrastructure.config;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.DeterministicTestEmailNotificationSenderAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.EmailNotificationDeliveryAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.SmtpEmailNotificationSenderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEmailConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(NotificationEmailConfiguration.class)
        .withPropertyValues("app.notification.email.from=noreply@example.test",
            "app.notification.email.connect-timeout=PT1S", "app.notification.email.read-timeout=PT1S");

    @Test void disabledModeCreatesExactlyOneExplicitlyDisabledSender() {
        context.withPropertyValues("app.notification.email.enabled=false", "app.notification.email.mode=test",
            "app.notification.email.provider=test").run(started -> {
                assertThat(started).hasSingleBean(EmailNotificationSenderPort.class);
                assertThat(started.getBean(EmailNotificationSenderPort.class)).isInstanceOf(EmailNotificationDeliveryAdapter.class);
            });
    }

    @Test void enabledTestModeCreatesSafeNonAcceptingSender() {
        context.withPropertyValues("app.notification.email.enabled=true", "app.notification.email.mode=test",
            "app.notification.email.provider=test").run(started -> {
                assertThat(started).hasSingleBean(EmailNotificationSenderPort.class);
                assertThat(started.getBean(EmailNotificationSenderPort.class)).isInstanceOf(DeterministicTestEmailNotificationSenderAdapter.class);
            });
    }

    @Test void enabledProductionModeCreatesRealSmtpSender() {
        context.withPropertyValues("app.notification.email.enabled=true", "app.notification.email.mode=production",
            "app.notification.email.provider=smtp", "app.notification.email.smtp.host=localhost",
            "app.notification.email.smtp.port=2525", "app.notification.email.smtp.tls-mode=none",
            "app.notification.email.smtp.authentication-required=false").run(started -> {
                assertThat(started).hasSingleBean(EmailNotificationSenderPort.class);
                assertThat(started.getBean(EmailNotificationSenderPort.class)).isInstanceOf(SmtpEmailNotificationSenderAdapter.class);
            });
    }

    @Test void invalidEnabledConfigurationFailsAtStartup() {
        context.withPropertyValues("app.notification.email.enabled=true", "app.notification.email.mode=production",
            "app.notification.email.provider=smtp", "app.notification.email.smtp.host=",
            "app.notification.email.smtp.port=587", "app.notification.email.smtp.tls-mode=starttls",
            "app.notification.email.smtp.authentication-required=true").run(started ->
                assertThat(started).hasFailed());
    }
}
