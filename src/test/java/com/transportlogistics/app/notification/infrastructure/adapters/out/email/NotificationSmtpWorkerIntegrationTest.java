package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.in.NotificationEscalationUseCase;
import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryClaimService;
import com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryWorker;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import com.transportlogistics.app.notification.infrastructure.adapters.out.persistence.NotificationDeliveryAttemptJpaRepository;
import com.transportlogistics.app.notification.infrastructure.adapters.out.persistence.NotificationDeliveryAttemptPersistenceAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.persistence.NotificationJpaRepository;
import com.transportlogistics.app.notification.infrastructure.adapters.out.persistence.NotificationPersistenceAdapter;
import com.transportlogistics.app.notification.infrastructure.config.NotificationEmailProperties;
import com.transportlogistics.app.notification.support.LocalSmtpTestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationPersistenceAdapter.class, NotificationDeliveryAttemptPersistenceAdapter.class,
    NotificationEmailDeliveryClaimService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationSmtpWorkerIntegrationTest {
    @Autowired NotificationPersistenceAdapter notifications;
    @Autowired NotificationDeliveryAttemptPersistenceAdapter attempts;
    @Autowired NotificationEmailDeliveryClaimService claims;
    @Autowired NotificationDeliveryAttemptJpaRepository attemptJpa;
    @Autowired NotificationJpaRepository notificationJpa;
    @Autowired PlatformTransactionManager transactions;

    @AfterEach void clean() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            attemptJpa.deleteAll();
            notificationJpa.deleteEscalationChildren();
            notificationJpa.deleteAll();
        });
    }

    @Test void realSmtpAcceptancePersistsSucceededAttemptAndSentNotification() throws Exception {
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.ACCEPT)) {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-21T10:00:00Z"));
            Notification pending = savePending(clock);
            NotificationEscalationUseCase escalation = mock(NotificationEscalationUseCase.class);

            worker(sender(server.port(), false), escalation, clock).processDue();

            assertThat(notifications.findById(pending.id()).orElseThrow().status()).isEqualTo(NotificationStatus.SENT);
            assertThat(attempts.findByNotificationId(pending.id())).singleElement().satisfies(attempt -> {
                assertThat(attempt.state().name()).isEqualTo("SUCCEEDED");
                assertThat(attempt.providerMessageId()).isNotBlank();
            });
            verify(escalation, never()).escalateIfDue(pending.id(), OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        }
    }

    @Test void transientSmtpFailureSchedulesRetryThenASecondRealAttemptSucceeds() throws Exception {
        try (var server = new LocalSmtpTestServer(LocalSmtpTestServer.Scenario.TEMPORARY_RECIPIENT_REJECTION,
            LocalSmtpTestServer.Scenario.ACCEPT)) {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-21T10:00:00Z"));
            Notification pending = savePending(clock);
            NotificationEscalationUseCase escalation = mock(NotificationEscalationUseCase.class);
            NotificationEmailDeliveryWorker worker = worker(sender(server.port(), false), escalation, clock);

            worker.processDue();
            Notification retrying = notifications.findById(pending.id()).orElseThrow();
            assertThat(retrying.status()).isEqualTo(NotificationStatus.PENDING);
            assertThat(retrying.nextDeliveryAt()).isEqualTo(OffsetDateTime.parse("2026-08-21T10:01:00Z"));
            clock.advance(Duration.ofMinutes(2));
            worker.processDue();

            assertThat(notifications.findById(pending.id()).orElseThrow().status()).isEqualTo(NotificationStatus.SENT);
            assertThat(attempts.findByNotificationId(pending.id())).hasSize(2);
            assertThat(attempts.findByNotificationId(pending.id()).get(1).state().name()).isEqualTo("SUCCEEDED");
        }
    }

    @Test void permanentAndAuthenticationFailuresAreTerminalAndInvokeEscalationWithoutFalseSent() throws Exception {
        verifyTerminal(LocalSmtpTestServer.Scenario.PERMANENT_RECIPIENT_REJECTION, false);
        verifyTerminal(LocalSmtpTestServer.Scenario.AUTHENTICATION_REJECTION, true);
    }

    @Test void disabledSenderCannotMarkNotificationSent() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-21T10:00:00Z"));
        Notification pending = savePending(clock);
        NotificationEscalationUseCase escalation = mock(NotificationEscalationUseCase.class);
        worker(new EmailNotificationDeliveryAdapter(), escalation, clock).processDue();
        assertThat(notifications.findById(pending.id()).orElseThrow().status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(attempts.findByNotificationId(pending.id())).singleElement()
            .satisfies(attempt -> assertThat(attempt.errorCode()).isEqualTo("EMAIL_DISABLED"));
    }

    private void verifyTerminal(LocalSmtpTestServer.Scenario scenario, boolean auth) throws Exception {
        clean();
        try (var server = new LocalSmtpTestServer(scenario)) {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-21T10:00:00Z"));
            Notification pending = savePending(clock);
            NotificationEscalationUseCase escalation = mock(NotificationEscalationUseCase.class);
            worker(sender(server.port(), auth), escalation, clock).processDue();
            Notification failed = notifications.findById(pending.id()).orElseThrow();
            assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);
            assertThat(failed.nextDeliveryAt()).isNull();
            assertThat(attempts.findByNotificationId(pending.id())).hasSize(1);
            verify(escalation).escalateIfDue(pending.id(), OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        }
    }

    private Notification savePending(MutableClock clock) {
        return new TransactionTemplate(transactions).execute(status -> notifications.save(Notification.createPending(
            null, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL, "ops@example.test",
            NotificationSeverity.WARNING, "Trip delayed", "Plain text body", null, null, null,
            OffsetDateTime.ofInstant(clock.instant().minusSeconds(60), ZoneOffset.UTC), null)));
    }

    private NotificationEmailDeliveryWorker worker(EmailNotificationSenderPort sender,
                                                    NotificationEscalationUseCase escalation, Clock clock) {
        return new NotificationEmailDeliveryWorker(notifications, claims, escalation, sender, clock,
            "noreply@example.test", Duration.ofSeconds(2));
    }

    private SmtpEmailNotificationSenderAdapter sender(int port, boolean auth) {
        NotificationEmailProperties email = new NotificationEmailProperties();
        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost("127.0.0.1"); mail.setPort(port);
        if (auth) { mail.setUsername("user"); mail.setPassword("secret"); }
        Properties properties = mail.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(auth));
        properties.setProperty("mail.smtp.connectiontimeout", "250");
        properties.setProperty("mail.smtp.timeout", "250");
        properties.setProperty("mail.smtp.writetimeout", "250");
        return new SmtpEmailNotificationSenderAdapter(mail, email);
    }

    static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
