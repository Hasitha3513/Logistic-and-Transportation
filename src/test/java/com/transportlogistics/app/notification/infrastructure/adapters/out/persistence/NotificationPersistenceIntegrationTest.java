package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationRulePersistenceAdapter.class, NotificationPersistenceAdapter.class,
    NotificationTemplatePersistenceAdapter.class, NotificationRuleExecutionPersistenceAdapter.class,
    NotificationDeliveryAttemptPersistenceAdapter.class,
    com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryClaimService.class})
class NotificationPersistenceIntegrationTest {

    @Autowired
    private NotificationRulePersistenceAdapter ruleAdapter;

    @Autowired
    private NotificationPersistenceAdapter notificationAdapter;

    @Autowired
    private NotificationTemplatePersistenceAdapter templateAdapter;

    @Autowired
    private NotificationRuleExecutionPersistenceAdapter executionAdapter;

    @Autowired
    private NotificationDeliveryAttemptPersistenceAdapter attemptAdapter;

    @Autowired
    private com.transportlogistics.app.notification.application.service.NotificationEmailDeliveryClaimService claimService;

    @Autowired
    private NotificationDeliveryAttemptJpaRepository attemptJpaRepository;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private NotificationRulePolicyJpaRepository policyJpaRepository;

    @Autowired
    private NotificationRuleExecutionJpaRepository executionJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Should save and retrieve notification rules by event type")
    void notificationRule_saveAndFindByEventType_succeeds() {
        NotificationRule rule = NotificationRule.create(
            "Delay Warning",
            "Notify on delays",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.WARNING
        );

        NotificationRule saved = ruleAdapter.save(rule);
        assertThat(saved.id()).isEqualTo(rule.id());

        List<NotificationRule> matching = ruleAdapter.findByEventTypeAndEnabledTrue("trip_delay_recorded");
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).name()).isEqualTo("Delay Warning");
        assertThat(matching.get(0).templateCode()).isEqualTo("TRIP_DELAY");
        assertThat(matching.get(0).policy().suppressionWindowMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should load the single active compatible system template")
    void template_findActiveCompatible_succeeds() {
        var template = templateAdapter.findActiveCompatible(
            "TRIP_DELAY", "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP);

        assertThat(template).isPresent();
        assertThat(template.orElseThrow().version()).isEqualTo(1);
        assertThat(templateAdapter.findActive(null, null)).hasSize(16);
    }

    @Test
    @DisplayName("Should save, query, and mark notifications as read")
    void notification_saveAndQueryRecipients_succeeds() {
        UUID eventId = UUID.randomUUID();
        var template = templateAdapter.findActiveCompatible(
            "TRIP_DELAY", "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP).orElseThrow();
        Notification notification = Notification.createPending(
            null,
            eventId,
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            "user1",
            NotificationSeverity.WARNING,
            "Trip Delayed",
            "Delay of 15 min",
            template.id(),
            template.version(),
            "/trips/1"
        ).markSent();

        notificationAdapter.save(notification);

        boolean exists = notificationAdapter.existsByEventIdAndRuleIdAndRecipient(eventId, null, "user1");
        assertThat(exists).isTrue();

        List<Notification> list = notificationAdapter.findByRecipientsOrderByCreatedAtDesc(Set.of("user1"), 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).templateId()).isEqualTo(template.id());
        assertThat(list.get(0).templateVersion()).isEqualTo(1);

        long unread = notificationAdapter.countUnreadByRecipients(Set.of("user1"));
        assertThat(unread).isEqualTo(1L);

        int updated = notificationAdapter.markAllAsReadForRecipients(Set.of("user1"));
        assertThat(updated).isEqualTo(1);

        long unreadAfter = notificationAdapter.countUnreadByRecipients(Set.of("user1"));
        assertThat(unreadAfter).isEqualTo(0L);
    }

    @Test
    void executionAudit_saveAndSuppressionLookup_succeeds() {
        NotificationRule rule = ruleAdapter.save(NotificationRule.create("Delay audit", null,
            "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP, RecipientType.USER, "user1",
            true, NotificationSeverity.WARNING));
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        NotificationRuleExecution execution = NotificationRuleExecution.completed(UUID.randomUUID(),
            rule.eventType(), "Trip", UUID.randomUUID(), rule.id(), "user1", rule.channel(),
            NotificationRuleExecutionOutcome.ACCEPTED, "a".repeat(64), null, null, null, now);

        executionAdapter.save(execution);

        assertThat(executionAdapter.existsByExecutionKey(execution.executionKey())).isTrue();
        assertThat(executionAdapter.findLatestAccepted(execution.suppressionKey(), now.minusMinutes(15)))
            .contains(execution);
        assertThat(executionAdapter.findLatestAccepted(execution.suppressionKey(), now)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void policyRowLock_serializesEquivalentSuppressionEvaluation() throws Exception {
        TransactionTemplate setup = new TransactionTemplate(transactionManager);
        NotificationRule rule = setup.execute(status -> ruleAdapter.save(NotificationRule.create("Concurrent delay", null,
            "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP, RecipientType.USER, "user1",
            true, NotificationSeverity.WARNING)));
        String suppressionKey = "b".repeat(64);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> atomicAccept(rule, suppressionKey, now, firstLocked, releaseFirst));
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Boolean> second = executor.submit(() -> atomicAccept(rule, suppressionKey, now, null, null));
            releaseFirst.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            setup.executeWithoutResult(status -> {
                executionJpaRepository.deleteAll();
                ruleAdapter.deleteById(rule.id());
            });
        }
    }

    @Test
    void quietPolicyAndPendingNextDelivery_roundTrip() {
        var policy = new com.transportlogistics.app.notification.domain.model.NotificationRulePolicy(true,
            LocalTime.of(22, 0), LocalTime.of(6, 0), Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), 30,
            true, 10, RecipientType.ROLE, "OPERATIONS");
        NotificationRule rule = ruleAdapter.save(NotificationRule.create("Quiet email", null,
            "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL, RecipientType.EMAIL_ADDRESS, "ops@example.test",
            "TRIP_DELAY", policy, true, NotificationSeverity.WARNING));
        assertThat(ruleAdapter.findById(rule.id()).orElseThrow().policy()).isEqualTo(policy);

        OffsetDateTime created = OffsetDateTime.parse("2026-08-21T16:30:00Z");
        OffsetDateTime due = OffsetDateTime.parse("2026-08-22T00:30:00Z");
        Notification pending = Notification.createPending(rule.id(), UUID.randomUUID(), rule.eventType(),
            rule.channel(), "ops@example.test", NotificationSeverity.WARNING, "Quiet", "Queued",
            null, null, null, created, due);
        notificationAdapter.save(pending);
        assertThat(notificationAdapter.findById(pending.id()).orElseThrow().nextDeliveryAt()).isEqualTo(due);
    }

    @Test
    void deliveryAttemptAndEscalationLinkage_roundTrip() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        Notification email = Notification.createPending(null, UUID.randomUUID(), "TRIP_DELAY_RECORDED",
            NotificationChannel.EMAIL, "ops@example.test", NotificationSeverity.WARNING, "Subject", "Body",
            null, null, null, now.minusMinutes(1), null);
        notificationAdapter.save(email);
        var attempt = com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt
            .start(email.id(), 1, now, now)
            .fail(now.plusSeconds(1), com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory.HTTP_429,
                "HTTP_429", "throttled\nsecret removed");
        attemptAdapter.save(attempt);
        assertThat(attemptAdapter.findByNotificationId(email.id())).singleElement().satisfies(saved -> {
            assertThat(saved.attemptNumber()).isEqualTo(1);
            assertThat(saved.errorMessage()).doesNotContain("\n");
        });

        Notification failed = notificationAdapter.save(email.markFailed("HTTP_429: throttled"));
        Notification child = notificationAdapter.save(Notification.createEscalation(failed, "admin", "throttled", now));
        assertThat(notificationAdapter.findById(child.id()).orElseThrow().parentNotificationId()).isEqualTo(email.id());
        assertThat(notificationAdapter.findById(child.id()).orElseThrow().escalationLevel()).isEqualTo(1);
    }

    @Test
    void duePendingEmailQueryRecoversPersistedWork() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        Notification due = Notification.createPending(null, UUID.randomUUID(), "TRIP_DELAY_RECORDED",
            NotificationChannel.EMAIL, "recover@example.test", NotificationSeverity.WARNING, "Subject", "Body",
            null, null, null, now.minusMinutes(2), now.minusMinutes(1));
        notificationAdapter.save(due);
        assertThat(notificationAdapter.findDuePendingEmails(now, 10)).extracting(Notification::id).contains(due.id());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentClaimsCreateExactlyOneProviderAttempt() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        Notification pending = transaction.execute(status -> notificationAdapter.save(Notification.createPending(
            null, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "concurrent@example.test", NotificationSeverity.WARNING, "Subject", "Body", null, null, null,
            now.minusMinutes(1), null)));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> claimService.claim(pending.id(), now).isPresent());
            var second = executor.submit(() -> claimService.claim(pending.id(), now).isPresent());
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
            assertThat(attemptAdapter.findByNotificationId(pending.id())).hasSize(1);
        } finally {
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> {
                attemptJpaRepository.deleteAll();
                notificationJpaRepository.deleteById(pending.id());
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void uniqueNotificationAttemptNumberIsEnforced() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        Notification pending = transaction.execute(status -> notificationAdapter.save(Notification.createPending(
            null, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "unique@example.test", NotificationSeverity.WARNING, "Subject", "Body", null, null, null,
            now.minusMinutes(1), null)));
        transaction.executeWithoutResult(status -> attemptAdapter.save(
            com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt.start(
                pending.id(), 1, now, now)));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> transaction.executeWithoutResult(status ->
            attemptAdapter.save(com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt.start(
                pending.id(), 1, now, now))))).isNotNull();
        transaction.executeWithoutResult(status -> {
            attemptJpaRepository.deleteAll();
            notificationJpaRepository.deleteById(pending.id());
        });
    }

    private boolean atomicAccept(NotificationRule rule, String key, OffsetDateTime now,
                                 CountDownLatch locked, CountDownLatch release) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Boolean.TRUE.equals(transaction.execute(status -> {
            policyJpaRepository.findByRuleIdForUpdate(rule.id()).orElseThrow();
            if (locked != null) {
                locked.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Lock test timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            if (executionAdapter.findLatestAccepted(key, now.minusMinutes(15)).isPresent()) return false;
            executionAdapter.save(NotificationRuleExecution.completed(UUID.randomUUID(), rule.eventType(), "Trip",
                UUID.randomUUID(), rule.id(), "user1", rule.channel(), NotificationRuleExecutionOutcome.ACCEPTED,
                key, null, null, null, now));
            return true;
        }));
    }
}
