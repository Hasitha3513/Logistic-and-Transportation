package com.transportlogistics.app.notification.infrastructure.testing;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class E2eDeterministicEmailSenderTest {
    private final E2eDeterministicEmailSender sender = new E2eDeterministicEmailSender();

    @Test
    void terminalRecipientNeverReportsAcceptance() throws Exception {
        var result = sender.send(request("e2e-terminal-case@example.test", 1));

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCategory()).isEqualTo(EmailDeliveryErrorCategory.INVALID_RECIPIENT);
        assertThat(result.providerMessageId()).isNull();
    }

    @Test
    void retryRecipientFailsOnceThenReturnsProviderEvidence() throws Exception {
        var first = sender.send(request("e2e-retry-case@example.test", 1));
        var second = sender.send(request("e2e-retry-case@example.test", 2));

        assertThat(first.accepted()).isFalse();
        assertThat(first.errorCategory()).isEqualTo(EmailDeliveryErrorCategory.CONNECTION);
        assertThat(second.accepted()).isTrue();
        assertThat(second.providerMessageId()).startsWith("e2e-provider-");
    }

    private EmailNotificationSenderPort.SendRequest request(String recipient, int attempt) {
        UUID notificationId = UUID.randomUUID();
        return new EmailNotificationSenderPort.SendRequest(notificationId, notificationId + ":" + attempt,
            "noreply@example.test", recipient, "Subject", "Body", Duration.ofSeconds(1));
    }
}
