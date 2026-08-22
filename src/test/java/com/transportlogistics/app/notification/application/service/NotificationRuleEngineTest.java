package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRecipientDirectoryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRulePolicyRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import com.transportlogistics.app.notification.domain.model.NotificationTemplateRenderer;
import com.transportlogistics.app.notification.domain.model.NotificationQuietHoursEvaluator;
import com.transportlogistics.app.notification.domain.model.NotificationRulePolicy;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationRuleEngineTest {

    private NotificationRuleRepository ruleRepository;
    private NotificationRepository notificationRepository;
    private NotificationDeliveryPort inAppDeliveryPort;
    private NotificationDeliveryPort emailDeliveryPort;
    private NotificationTemplateRepository templateRepository;
    private NotificationRulePolicyRepository policyRepository;
    private NotificationRuleExecutionRepository executionRepository;
    private NotificationRecipientDirectoryPort recipientDirectory;
    private NotificationRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(NotificationRuleRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        inAppDeliveryPort = mock(NotificationDeliveryPort.class);
        emailDeliveryPort = mock(NotificationDeliveryPort.class);
        templateRepository = mock(NotificationTemplateRepository.class);
        policyRepository = mock(NotificationRulePolicyRepository.class);
        executionRepository = mock(NotificationRuleExecutionRepository.class);
        recipientDirectory = mock(NotificationRecipientDirectoryPort.class);

        Map<NotificationChannel, NotificationDeliveryPort> ports = new EnumMap<>(NotificationChannel.class);
        ports.put(NotificationChannel.IN_APP, inAppDeliveryPort);
        ports.put(NotificationChannel.EMAIL, emailDeliveryPort);

        var recipientResolver = new NotificationRecipientResolver(recipientDirectory);
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC);
        ruleEngine = new NotificationRuleEngine(ruleRepository, policyRepository, executionRepository,
            notificationRepository, templateRepository, recipientResolver, new NotificationTemplateRenderer(),
            new NotificationQuietHoursEvaluator(clock, ZoneOffset.UTC),
            new NotificationSuppressionEvaluator(executionRepository), ports, clock);
        when(policyRepository.findByRuleIdForUpdate(any())).thenReturn(Optional.empty());
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipientDirectory.activeRoleExists(any())).thenReturn(true);
        when(recipientDirectory.findActiveRoleMembers(any())).thenReturn(List.of(
            new NotificationRecipientDirectoryPort.RecipientUser("dispatcher", "dispatcher@example.test")));
        when(templateRepository.findActiveCompatible(any(), any(), any())).thenAnswer(invocation -> Optional.of(
            template(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))));
    }

    @Test
    void processEvent_matchingInAppRule_createsAndDispatchesNotification() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "TRIP_DELAY_RECORDED",
            "Trip",
            UUID.randomUUID(),
            OperationalNotificationEvent.Severity.WARNING,
            "Delay Alert",
            "Trip delayed by 25 mins",
            delayMetadata()
        );

        NotificationRule rule = NotificationRule.create(
            "Delay Alert Rule",
            "Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.WARNING
        );

        when(ruleRepository.findByEventTypeAndEnabledTrue("TRIP_DELAY_RECORDED")).thenReturn(List.of(rule));
        when(notificationRepository.existsByEventIdAndRuleIdAndRecipient(any(), any(), any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ruleEngine.processEvent(event);

        verify(inAppDeliveryPort).deliver(any(Notification.class));
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void processEvent_idempotency_preventsDuplicateNotification() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "TRIP_DELAY_RECORDED",
            "Trip",
            UUID.randomUUID(),
            OperationalNotificationEvent.Severity.WARNING,
            "Delay Alert",
            "Trip delayed by 25 mins",
            delayMetadata()
        );

        NotificationRule rule = NotificationRule.create(
            "Delay Alert Rule",
            "Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.WARNING
        );

        when(ruleRepository.findByEventTypeAndEnabledTrue("TRIP_DELAY_RECORDED")).thenReturn(List.of(rule));
        when(executionRepository.existsByExecutionKey(anyString())).thenReturn(true);

        ruleEngine.processEvent(event);

        verifyNoInteractions(inAppDeliveryPort);
    }

    @Test
    void processEvent_immediateEmail_isPersistedPendingForDurableWorker() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "VEHICLE_MAINTENANCE_DUE",
            "Vehicle",
            UUID.randomUUID(),
            OperationalNotificationEvent.Severity.CRITICAL,
            "Maintenance Blocked",
            "Vehicle in maintenance",
            Map.of(
                "vehicleId", UUID.randomUUID().toString(),
                "vehicleRegistration", "WP-1234",
                "maintenanceType", "SERVICE",
                "scheduledStart", "2026-08-22T08:00:00Z",
                "scheduledEnd", "2026-08-22T10:00:00Z")
        );

        NotificationRule rule = NotificationRule.create(
            "Maintenance Email Rule",
            "Desc",
            "VEHICLE_MAINTENANCE_DUE",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "manager@company.com",
            true,
            NotificationSeverity.CRITICAL
        );

        when(ruleRepository.findByEventTypeAndEnabledTrue("VEHICLE_MAINTENANCE_DUE")).thenReturn(List.of(rule));
        when(notificationRepository.existsByEventIdAndRuleIdAndRecipient(any(), any(), any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ruleEngine.processEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification lastSaved = captor.getValue();
        assertThat(lastSaved.status()).isEqualTo(com.transportlogistics.app.notification.domain.model.NotificationStatus.PENDING);
        assertThat(lastSaved.nextDeliveryAt()).isNull();
        verifyNoInteractions(emailDeliveryPort);
    }

    @Test
    void processEvent_missingRequiredMetadata_doesNotDeliver() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "TRIP_DELAY_RECORDED", "Trip", UUID.randomUUID(),
            OperationalNotificationEvent.Severity.WARNING, "Ignored", "Ignored", Map.of());
        NotificationRule rule = NotificationRule.create(
            "Delay", null, "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP,
            RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.WARNING);
        when(ruleRepository.findByEventTypeAndEnabledTrue("TRIP_DELAY_RECORDED")).thenReturn(List.of(rule));

        ruleEngine.processEvent(event);

        verifyNoInteractions(inAppDeliveryPort);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void processEvent_withAcceptedExecutionInWindow_recordsSuppressedWithoutNotification() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "TRIP_DELAY_RECORDED", "Trip", UUID.randomUUID(), OperationalNotificationEvent.Severity.WARNING,
            "Delay", "Delay", delayMetadata());
        NotificationRule rule = NotificationRule.create("Delay", null, "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP, RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.WARNING);
        UUID controllingNotification = UUID.randomUUID();
        var prior = NotificationRuleExecution.completed(UUID.randomUUID(), event.eventType(), event.aggregateType(),
            event.aggregateId(), rule.id(), "dispatcher", rule.channel(), NotificationRuleExecutionOutcome.ACCEPTED,
            "a".repeat(64), controllingNotification, null, null, OffsetDateTime.parse("2026-08-21T09:55:00Z"));
        when(ruleRepository.findByEventTypeAndEnabledTrue(event.eventType())).thenReturn(List.of(rule));
        when(executionRepository.findLatestAccepted(anyString(), any())).thenReturn(Optional.of(prior));

        ruleEngine.processEvent(event);

        verify(notificationRepository, never()).save(any());
        ArgumentCaptor<NotificationRuleExecution> execution = ArgumentCaptor.forClass(NotificationRuleExecution.class);
        verify(executionRepository).save(execution.capture());
        assertThat(execution.getValue().outcome()).isEqualTo(NotificationRuleExecutionOutcome.SUPPRESSED);
        assertThat(execution.getValue().controllingNotificationId()).isEqualTo(controllingNotification);
    }

    @Test
    void processEvent_emailDuringQuietHours_isDurablyQueuedWithoutDelivery() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "VEHICLE_MAINTENANCE_DUE", "Vehicle", UUID.randomUUID(), OperationalNotificationEvent.Severity.WARNING,
            "Maintenance", "Maintenance", Map.of("vehicleId", UUID.randomUUID().toString(),
                "vehicleRegistration", "WP-1234", "maintenanceType", "SERVICE",
                "scheduledStart", "2026-08-21T10:00:00Z", "scheduledEnd", "2026-08-21T11:00:00Z"));
        var policy = new NotificationRulePolicy(true, LocalTime.of(0, 0), LocalTime.of(23, 0),
            Set.of(DayOfWeek.FRIDAY), 0);
        NotificationRule rule = NotificationRule.create("Maintenance", null, event.eventType(), NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS, "manager@example.test", "VEHICLE_MAINTENANCE_DUE", policy,
            true, NotificationSeverity.WARNING);
        when(ruleRepository.findByEventTypeAndEnabledTrue(event.eventType())).thenReturn(List.of(rule));
        when(policyRepository.findByRuleIdForUpdate(rule.id())).thenReturn(Optional.of(policy));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ruleEngine.processEvent(event);

        verifyNoInteractions(emailDeliveryPort);
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertThat(notification.getValue().status()).isEqualTo(com.transportlogistics.app.notification.domain.model.NotificationStatus.PENDING);
        assertThat(notification.getValue().nextDeliveryAt()).isEqualTo("2026-08-21T23:00Z");
    }

    @Test
    void criticalEventsBypassTimeSuppressionWhileStableEventIdRemainsIdempotent() {
        Map<String, String> metadata = Map.of("vehicleId", UUID.randomUUID().toString(),
            "vehicleRegistration", "WP-9999", "maintenanceType", "SERVICE",
            "scheduledStart", "2026-08-21T10:00:00Z", "scheduledEnd", "2026-08-21T11:00:00Z");
        OperationalNotificationEvent first = OperationalNotificationEvent.of("VEHICLE_MAINTENANCE_DUE", "Vehicle",
            UUID.randomUUID(), OperationalNotificationEvent.Severity.CRITICAL, "Critical", "Critical", metadata);
        NotificationRule rule = NotificationRule.create("Critical", null, first.eventType(), NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS, "manager@example.test", true, NotificationSeverity.WARNING);
        when(ruleRepository.findByEventTypeAndEnabledTrue(first.eventType())).thenReturn(List.of(rule));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ruleEngine.processEvent(first);
        OperationalNotificationEvent second = OperationalNotificationEvent.of(first.eventType(), "Vehicle",
            first.aggregateId(), OperationalNotificationEvent.Severity.CRITICAL, "Critical", "Critical", metadata);
        ruleEngine.processEvent(second);

        verifyNoInteractions(emailDeliveryPort);
        verify(executionRepository, never()).findLatestAccepted(anyString(), any());

        when(executionRepository.existsByExecutionKey(anyString())).thenReturn(true);
        ruleEngine.processEvent(first);
        verifyNoInteractions(emailDeliveryPort);
    }

    @Test
    void zeroRecipientsAndTemplateFailureAreAuditedWithoutNotification() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of("TRIP_DELAY_RECORDED", "Trip",
            UUID.randomUUID(), OperationalNotificationEvent.Severity.WARNING, "Delay", "Delay", delayMetadata());
        NotificationRule rule = NotificationRule.create("Delay", null, event.eventType(), NotificationChannel.IN_APP,
            RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.WARNING);
        when(ruleRepository.findByEventTypeAndEnabledTrue(event.eventType())).thenReturn(List.of(rule));
        when(recipientDirectory.findActiveRoleMembers("DISPATCHER")).thenReturn(List.of());

        ruleEngine.processEvent(event);

        ArgumentCaptor<NotificationRuleExecution> execution = ArgumentCaptor.forClass(NotificationRuleExecution.class);
        verify(executionRepository).save(execution.capture());
        assertThat(execution.getValue().outcome()).isEqualTo(NotificationRuleExecutionOutcome.NO_RECIPIENT);
        verify(notificationRepository, never()).save(any());

        reset(executionRepository);
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(Optional.empty()).when(templateRepository).findActiveCompatible(any(), any(), any());
        ruleEngine.processEvent(OperationalNotificationEvent.of(event.eventType(), "Trip", UUID.randomUUID(),
            OperationalNotificationEvent.Severity.WARNING, "Delay", "Delay", delayMetadata()));
        verify(executionRepository).save(argThat(value ->
            value.outcome() == NotificationRuleExecutionOutcome.TEMPLATE_DATA_MISSING));
    }

    @Test
    void policyFailureDoesNotEscapeEngineAndCreatesFailedAudit() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of("TRIP_DELAY_RECORDED", "Trip",
            UUID.randomUUID(), OperationalNotificationEvent.Severity.WARNING, "Delay", "Delay", delayMetadata());
        NotificationRule rule = NotificationRule.create("Delay", null, event.eventType(), NotificationChannel.IN_APP,
            RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.WARNING);
        when(ruleRepository.findByEventTypeAndEnabledTrue(event.eventType())).thenReturn(List.of(rule));
        doThrow(new IllegalStateException("bad\nstate")).when(templateRepository)
            .findActiveCompatible(any(), any(), any());

        org.assertj.core.api.Assertions.assertThatCode(() -> ruleEngine.processEvent(event)).doesNotThrowAnyException();
        verify(executionRepository).save(argThat(value -> value.outcome() == NotificationRuleExecutionOutcome.FAILED
            && "bad state".equals(value.failureMessage())));
    }

    private Map<String, String> delayMetadata() {
        return Map.of(
            "tripId", UUID.randomUUID().toString(),
            "tripNumber", "TRIP-001",
            "delayMinutes", "25",
            "reason", "Traffic",
            "relatedRoute", "/trips/123");
    }

    private NotificationTemplate template(String code, String eventType, NotificationChannel channel) {
        return new NotificationTemplate(UUID.randomUUID(), code, code, eventType, channel,
            "{{severity}} alert", "Event at {{eventTime}}", 1, true,
            java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
    }
}
