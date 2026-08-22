package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRecipientDirectoryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationRuleServiceTest {

    private NotificationRuleRepository ruleRepository;
    private NotificationTemplateRepository templateRepository;
    private NotificationRecipientDirectoryPort recipientDirectory;
    private NotificationRuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(NotificationRuleRepository.class);
        templateRepository = mock(NotificationTemplateRepository.class);
        recipientDirectory = mock(NotificationRecipientDirectoryPort.class);
        var recipientResolver = new NotificationRecipientResolver(recipientDirectory);
        ruleService = new NotificationRuleService(ruleRepository, templateRepository, recipientResolver);
        when(recipientDirectory.activeRoleExists(any())).thenReturn(true);
        when(recipientDirectory.findActiveUser(any())).thenReturn(Optional.of(
            new NotificationRecipientDirectoryPort.RecipientUser("user1", "user1@example.test")));
        when(templateRepository.findActiveCompatible(any(), any(), any())).thenAnswer(invocation -> Optional.of(
            template(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))));
    }

    @Test
    void createRule_savesAndReturnsRule() {
        NotificationRuleUseCase.CreateRuleCommand cmd = new NotificationRuleUseCase.CreateRuleCommand(
            "Driver Leave Rule",
            "Alert on driver exception",
            "DRIVER_EXCEPTION_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "FLEET_MANAGER",
            "DRIVER_EXCEPTION",
            true,
            NotificationSeverity.INFO
        );

        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRule created = ruleService.createRule(cmd);

        assertThat(created.name()).isEqualTo("Driver Leave Rule");
        assertThat(created.eventType()).isEqualTo("DRIVER_EXCEPTION_RECORDED");
        verify(ruleRepository).save(any(NotificationRule.class));
    }

    @Test
    void updateRule_updatesFields() {
        UUID id = UUID.randomUUID();
        NotificationRule existing = NotificationRule.create(
            "Old Name",
            "Old Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.USER,
            "user1",
            true,
            NotificationSeverity.INFO
        );

        when(ruleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRuleUseCase.UpdateRuleCommand updateCmd = new NotificationRuleUseCase.UpdateRuleCommand(
            "New Name",
            "New Desc",
            "TRIP_INCIDENT_RECORDED",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "new@example.com",
            "TRIP_INCIDENT",
            null, null, null, null, null,
            true, 0, RecipientType.USER, "user1",
            false,
            NotificationSeverity.CRITICAL
        );

        NotificationRule updated = ruleService.updateRule(id, updateCmd);

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(updated.recipientValue()).isEqualTo("new@example.com");
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.severityThreshold()).isEqualTo(NotificationSeverity.CRITICAL);
    }

    @Test
    void enableAndDisableRule_modifiesState() {
        UUID id = UUID.randomUUID();
        NotificationRule rule = NotificationRule.create(
            "Rule", "Desc", "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP, RecipientType.USER, "user1", true, NotificationSeverity.INFO
        );

        when(ruleRepository.findById(id)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRule disabled = ruleService.disableRule(id);
        assertThat(disabled.enabled()).isFalse();

        when(ruleRepository.findById(id)).thenReturn(Optional.of(disabled));
        NotificationRule enabled = ruleService.enableRule(id);
        assertThat(enabled.enabled()).isTrue();
    }

    @Test
    void deleteRule_whenNotExists_throwsException() {
        UUID id = UUID.randomUUID();
        when(ruleRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> ruleService.deleteRule(id))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createRule_rejectsUnsupportedEvent() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "Unsupported", null, "FUEL_LIMIT_EXCEEDED", NotificationChannel.IN_APP,
            RecipientType.ROLE, "FLEET_MANAGER", "FUEL_LIMIT", true, NotificationSeverity.WARNING);

        assertThatThrownBy(() -> ruleService.createRule(command))
            .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
            .hasMessageContaining("Unsupported notification event");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRule_rejectsUnsupportedChannel() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "No channel", null, "TRIP_DELAY_RECORDED", null,
            RecipientType.ROLE, "FLEET_MANAGER", "TRIP_DELAY", true, NotificationSeverity.WARNING);

        assertThatThrownBy(() -> ruleService.createRule(command))
            .isInstanceOfSatisfying(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("NOTIFICATION_EVENT_UNSUPPORTED"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRule_rejectsIncompatibleTemplate() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "Wrong template", null, "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP,
            RecipientType.ROLE, "FLEET_MANAGER", "TRIP_INCIDENT", true, NotificationSeverity.WARNING);

        assertThatThrownBy(() -> ruleService.createRule(command))
            .isInstanceOfSatisfying(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("NOTIFICATION_TEMPLATE_INCOMPATIBLE"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRule_appliesCatalogueSuppressionDefaultWhenPolicyIsOmitted() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "Delay", null, "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP,
            RecipientType.ROLE, "FLEET_MANAGER", "TRIP_DELAY", true, NotificationSeverity.WARNING);
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(ruleService.createRule(command).policy().suppressionWindowMinutes()).isEqualTo(15);
    }

    @Test
    void createRule_rejectsQuietHoursForInAppChannel() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "Delay", null, "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP,
            RecipientType.ROLE, "FLEET_MANAGER", "TRIP_DELAY", true,
            LocalTime.of(22, 0), LocalTime.of(6, 0), Set.of(DayOfWeek.MONDAY), 15,
            true, NotificationSeverity.WARNING);

        assertThatThrownBy(() -> ruleService.createRule(command))
            .isInstanceOfSatisfying(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("NOTIFICATION_POLICY_INVALID"));
    }

    @Test
    void createRule_rejectsCriticalCapableEmailWithoutFallback() {
        var command = new NotificationRuleUseCase.CreateRuleCommand(
            "Email", null, "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS, "ops@example.test", "TRIP_DELAY", true,
            NotificationSeverity.INFO);

        assertThatThrownBy(() -> ruleService.createRule(command))
            .isInstanceOfSatisfying(com.transportlogistics.app.shared.domain.BusinessRuleException.class,
                error -> assertThat(error.code()).isEqualTo("NOTIFICATION_POLICY_INVALID"));
    }

    private NotificationTemplate template(String code, String eventType, NotificationChannel channel) {
        return new NotificationTemplate(UUID.randomUUID(), code, code, eventType, channel,
            "{{severity}} alert", "Event at {{eventTime}}", 1, true,
            java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
    }
}
