package com.transportlogistics.app.notification.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationConfigurationUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers.NotificationController;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers.NotificationConfigurationController;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers.NotificationRuleController;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.CreateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers.NotificationWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest {

    private MockMvc ruleMvc;
    private MockMvc notifMvc;
    private MockMvc configurationMvc;
    private NotificationRuleUseCase ruleUseCase;
    private NotificationUseCase notifUseCase;
    private NotificationConfigurationUseCase configurationUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ruleUseCase = mock(NotificationRuleUseCase.class);
        notifUseCase = mock(NotificationUseCase.class);
        configurationUseCase = mock(NotificationConfigurationUseCase.class);
        NotificationWebMapper mapper = new NotificationWebMapper();

        ruleMvc = MockMvcBuilders.standaloneSetup(new NotificationRuleController(ruleUseCase, mapper)).build();
        notifMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notifUseCase, mapper)).build();
        configurationMvc = MockMvcBuilders.standaloneSetup(
            new NotificationConfigurationController(configurationUseCase, mapper)).build();
    }

    @Test
    void listRules_returnsOk() throws Exception {
        when(ruleUseCase.listRules()).thenReturn(List.of(
            NotificationRule.create(
                "Rule 1", "Desc", "TRIP_DELAY_RECORDED", NotificationChannel.IN_APP, RecipientType.USER, "admin", true, NotificationSeverity.INFO
            )
        ));

        ruleMvc.perform(get("/notification-rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Rule 1"));
    }

    @Test
    void createRule_withValidPayload_returnsCreated() throws Exception {
        CreateNotificationRuleRequest req = new CreateNotificationRuleRequest(
            "Rule 1",
            "Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            "TRIP_DELAY",
            true,
            NotificationSeverity.WARNING
        );

        when(ruleUseCase.createRule(any())).thenReturn(
            NotificationRule.create(
                "Rule 1", "Desc", "TRIP_DELAY_RECORDED",
                NotificationChannel.IN_APP, RecipientType.ROLE, "DISPATCHER", "TRIP_DELAY", true,
                NotificationSeverity.WARNING
            )
        );

        ruleMvc.perform(post("/notification-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Rule 1"))
            .andExpect(jsonPath("$.templateCode").value("TRIP_DELAY"));
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(notifUseCase.getUnreadCount(any(), any())).thenReturn(5L);

        notifMvc.perform(get("/notifications/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markAsRead_returnsOk() throws Exception {
        UUID notifId = UUID.randomUUID();
        Notification notif = Notification.createPending(
            null, UUID.randomUUID(), "TRIP_DELAY", NotificationChannel.IN_APP, "user1",
            NotificationSeverity.WARNING, "Delayed", "Msg", null
        ).markSent().markRead();

        when(notifUseCase.markAsRead(eq(notifId), any(), any())).thenReturn(notif);

        notifMvc.perform(patch("/notifications/" + notifId + "/read"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void catalogue_returnsOnlyConfigurationMetadata() throws Exception {
        when(configurationUseCase.listEventCatalogue()).thenReturn(
            com.transportlogistics.app.notification.domain.model.NotificationEventCatalogue.all());

        configurationMvc.perform(get("/notification-event-catalogue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[?(@.eventType == 'TRIP_DELAY_RECORDED')]").exists());
    }

    @Test
    void templates_supportFilteringAndSingleTemplateLookup() throws Exception {
        UUID templateId = UUID.randomUUID();
        var template = new com.transportlogistics.app.notification.domain.model.NotificationTemplate(
            templateId, "TRIP_DELAY", "Trip delay", "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "Trip {{tripNumber}} delayed", "Delay: {{delayMinutes}} minutes. {{reason}}", 1, true,
            OffsetDateTime.now(), OffsetDateTime.now());
        when(configurationUseCase.listActiveTemplates("TRIP_DELAY_RECORDED", NotificationChannel.EMAIL))
            .thenReturn(List.of(template));
        when(configurationUseCase.getActiveTemplate(templateId)).thenReturn(Optional.of(template));

        configurationMvc.perform(get("/notification-templates")
                .param("eventType", "TRIP_DELAY_RECORDED").param("channel", "EMAIL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(templateId.toString()))
            .andExpect(jsonPath("$[0].code").value("TRIP_DELAY"))
            .andExpect(jsonPath("$[0].version").value(1));
        configurationMvc.perform(get("/notification-templates/" + templateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventType").value("TRIP_DELAY_RECORDED"));
    }
}
