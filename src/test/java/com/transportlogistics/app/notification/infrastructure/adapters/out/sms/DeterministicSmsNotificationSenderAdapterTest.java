package com.transportlogistics.app.notification.infrastructure.adapters.out.sms;

import com.transportlogistics.app.notification.application.ports.out.SmsNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicSmsNotificationSenderAdapterTest {
    private final DeterministicSmsNotificationSenderAdapter sender =
        new DeterministicSmsNotificationSenderAdapter();

    @Test
    void returnsDeterministicAcceptanceRetryableAndPermanentResults() {
        assertThat(send("+947700000001").accepted()).isTrue();
        assertThat(send("+947700000002").errorCategory()).isEqualTo(EmailDeliveryErrorCategory.TIMEOUT);
        assertThat(send("+947700000003").errorCategory()).isEqualTo(EmailDeliveryErrorCategory.PROVIDER_4XX);
    }

    private SmsNotificationSenderPort.SendResult send(String recipient) {
        UUID id = UUID.fromString("69000000-0000-0000-0000-000000000001");
        return sender.send(new SmsNotificationSenderPort.SendRequest(id, "stable-key", recipient,
            "safe message", Duration.ofSeconds(5)));
    }
}
