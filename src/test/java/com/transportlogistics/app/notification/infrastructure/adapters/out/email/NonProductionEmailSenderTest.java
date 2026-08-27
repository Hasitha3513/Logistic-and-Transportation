package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NonProductionEmailSenderTest {
    @Test void disabledAndTestSendersNeverReportProviderAcceptance() throws Exception {
        EmailNotificationSenderPort.SendRequest request = new EmailNotificationSenderPort.SendRequest(
            UUID.randomUUID(), "attempt:1", "from@example.test", "to@example.test", "subject", "body", Duration.ofSeconds(1));
        assertThat(new EmailNotificationDeliveryAdapter().send(request).accepted()).isFalse();
        assertThat(new DeterministicTestEmailNotificationSenderAdapter().send(request).accepted()).isFalse();
    }
}
