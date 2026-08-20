package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationRuleServiceTest {

    private NotificationRuleRepository ruleRepository;
    private NotificationRuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(NotificationRuleRepository.class);
        ruleService = new NotificationRuleService(ruleRepository);
    }

    @Test
    void createRule_savesAndReturnsRule() {
        NotificationRuleUseCase.CreateRuleCommand cmd = new NotificationRuleUseCase.CreateRuleCommand(
            "Driver Leave Rule",
            "Alert on driver exception",
            "DRIVER_EXCEPTION_CREATED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "FLEET_MANAGER",
            true,
            NotificationSeverity.INFO
        );

        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRule created = ruleService.createRule(cmd);

        assertThat(created.name()).isEqualTo("Driver Leave Rule");
        assertThat(created.eventType()).isEqualTo("DRIVER_EXCEPTION_CREATED");
        verify(ruleRepository).save(any(NotificationRule.class));
    }

    @Test
    void updateRule_updatesFields() {
        UUID id = UUID.randomUUID();
        NotificationRule existing = NotificationRule.create(
            "Old Name",
            "Old Desc",
            "EVENT_1",
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
            "EVENT_2",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "new@example.com",
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
            "Rule", "Desc", "EVENT", NotificationChannel.IN_APP, RecipientType.USER, "u1", true, NotificationSeverity.INFO
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
}
