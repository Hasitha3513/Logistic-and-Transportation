package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import com.transportlogistics.app.notification.infrastructure.config.NotificationEmailProperties;
import com.transportlogistics.app.notification.support.LocalSmtpTestServer;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpEmailNotificationSenderAdapterTest {
    @Test void acceptedSmtpMessageReturnsProviderEvidenceAndPreservesContent() throws Exception {
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.ACCEPT)) {
            var result = adapter(server.port(), false).send(request("recipient@example.test", "Fleet résumé"));
            assertThat(result.accepted()).isTrue();
            assertThat(result.providerMessageId()).isNotBlank();
            awaitMessage(server);
            assertThat(server.messages()).singleElement().satisfies(message ->
                assertThat(message).contains("Subject: =?UTF-8?", "recipient@example.test", "X-Idempotency-Key:",
                    "Plain-text operational message"));
        }
    }

    @Test void temporaryAndPermanentRecipientFailuresAreTyped() throws Exception {
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.TEMPORARY_RECIPIENT_REJECTION)) {
            var result = adapter(server.port(), false).send(request("recipient@example.test", "Subject"));
            assertThat(result.accepted()).isFalse();
            assertThat(result.errorCategory().retryable()).isTrue();
            assertThat(result.errorCode()).contains("450");
        }
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.PERMANENT_RECIPIENT_REJECTION)) {
            var result = adapter(server.port(), false).send(request("recipient@example.test", "Subject"));
            assertThat(result.accepted()).isFalse();
            assertThat(result.errorCategory().retryable()).isFalse();
        }
    }

    @Test void connectionTimeoutAuthenticationAndInvalidAddressAreTyped() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) { unusedPort = socket.getLocalPort(); }
        assertThat(adapter(unusedPort, false).send(request("recipient@example.test", "Subject")).errorCategory())
            .isEqualTo(EmailDeliveryErrorCategory.CONNECTION);
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.GREETING_TIMEOUT)) {
            assertThat(adapter(server.port(), false).send(request("recipient@example.test", "Subject")).errorCategory())
                .isEqualTo(EmailDeliveryErrorCategory.TIMEOUT);
        }
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.AUTHENTICATION_REJECTION)) {
            assertThat(adapter(server.port(), true).send(request("recipient@example.test", "Subject")).errorCategory())
                .isEqualTo(EmailDeliveryErrorCategory.AUTHENTICATION);
        }
        assertThat(adapter(unusedPort, false).send(request("not-an-address", "Subject")).errorCategory())
            .isEqualTo(EmailDeliveryErrorCategory.INVALID_RECIPIENT);
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.SENDER_REJECTION)) {
            var rejection = adapter(server.port(), false).send(request("recipient@example.test", "Subject"));
            assertThat(rejection.accepted()).isFalse();
            assertThat(rejection.errorCategory().retryable()).isFalse();
        }
    }

    @Test void interruptedThreadNeverContactsProviderOrReportsAcceptance() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> adapter(2525, false).send(request("recipient@example.test", "Subject")))
                .isInstanceOf(InterruptedException.class);
        } finally { Thread.interrupted(); }
    }

    private static SmtpEmailNotificationSenderAdapter adapter(int port, boolean auth) {
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setReplyTo("operations@example.test");
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1"); sender.setPort(port); sender.setDefaultEncoding("UTF-8");
        if (auth) { sender.setUsername("user"); sender.setPassword("secret"); }
        Properties mail = sender.getJavaMailProperties();
        mail.setProperty("mail.smtp.auth", Boolean.toString(auth));
        mail.setProperty("mail.smtp.connectiontimeout", "250");
        mail.setProperty("mail.smtp.timeout", "250");
        mail.setProperty("mail.smtp.writetimeout", "250");
        return new SmtpEmailNotificationSenderAdapter(sender, properties);
    }

    private static EmailNotificationSenderPort.SendRequest request(String to, String subject) {
        UUID id = UUID.randomUUID();
        return new EmailNotificationSenderPort.SendRequest(id, id + ":1", "noreply@example.test", to,
            subject, "Plain-text operational message", Duration.ofMillis(250));
    }

    private static void awaitMessage(LocalSmtpTestServer server) throws InterruptedException {
        for (int i = 0; i < 20 && server.messages().isEmpty(); i++) Thread.sleep(10);
    }
}
