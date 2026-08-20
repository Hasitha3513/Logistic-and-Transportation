package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationRuleEngineTest {

    private NotificationRuleRepository ruleRepository;
    private NotificationRepository notificationRepository;
    private NotificationDeliveryPort inAppDeliveryPort;
    private NotificationDeliveryPort emailDeliveryPort;
    private NotificationRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(NotificationRuleRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        inAppDeliveryPort = mock(NotificationDeliveryPort.class);
        emailDeliveryPort = mock(NotificationDeliveryPort.class);

        Map<NotificationChannel, NotificationDeliveryPort> ports = new EnumMap<>(NotificationChannel.class);
        ports.put(NotificationChannel.IN_APP, inAppDeliveryPort);
        ports.put(NotificationChannel.EMAIL, emailDeliveryPort);

        ruleEngine = new NotificationRuleEngine(ruleRepository, notificationRepository, ports);
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
            Map.of("relatedRoute", "/trips/123")
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
            Map.of()
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
        when(notificationRepository.existsByEventIdAndRuleIdAndRecipient(event.eventId(), rule.id(), "ROLE:DISPATCHER"))
            .thenReturn(true); // already exists!

        ruleEngine.processEvent(event);

        verifyNoInteractions(inAppDeliveryPort);
    }

    @Test
    void processEvent_emailDeliveryFailure_marksFailedStatus() {
        OperationalNotificationEvent event = OperationalNotificationEvent.of(
            "VEHICLE_MAINTENANCE_BLOCKED",
            "Vehicle",
            UUID.randomUUID(),
            OperationalNotificationEvent.Severity.CRITICAL,
            "Maintenance Blocked",
            "Vehicle in maintenance",
            Map.of()
        );

        NotificationRule rule = NotificationRule.create(
            "Maintenance Email Rule",
            "Desc",
            "VEHICLE_MAINTENANCE_BLOCKED",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "manager@company.com",
            true,
            NotificationSeverity.CRITICAL
        );

        when(ruleRepository.findByEventTypeAndEnabledTrue("VEHICLE_MAINTENANCE_BLOCKED")).thenReturn(List.of(rule));
        when(notificationRepository.existsByEventIdAndRuleIdAndRecipient(any(), any(), any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP connection timed out")).when(emailDeliveryPort).deliver(any());

        ruleEngine.processEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());
        Notification lastSaved = captor.getValue();
        assertThat(lastSaved.status()).isEqualTo(com.transportlogistics.app.notification.domain.model.NotificationStatus.FAILED);
        assertThat(lastSaved.failureReason()).contains("SMTP connection timed out");
    }
}
