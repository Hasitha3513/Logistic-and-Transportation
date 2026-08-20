package com.transportlogistics.app.notification.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers.NotificationController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest {

    private MockMvc ruleMvc;
    private MockMvc notifMvc;
    private NotificationRuleUseCase ruleUseCase;
    private NotificationUseCase notifUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ruleUseCase = mock(NotificationRuleUseCase.class);
        notifUseCase = mock(NotificationUseCase.class);
        NotificationWebMapper mapper = new NotificationWebMapper();

        ruleMvc = MockMvcBuilders.standaloneSetup(new NotificationRuleController(ruleUseCase, mapper)).build();
        notifMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notifUseCase, mapper)).build();
    }

    @Test
    void listRules_returnsOk() throws Exception {
        when(ruleUseCase.listRules()).thenReturn(List.of(
            NotificationRule.create(
                "Rule 1", "Desc", "EVENT", NotificationChannel.IN_APP, RecipientType.USER, "admin", true, NotificationSeverity.INFO
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
            true,
            NotificationSeverity.WARNING
        );

        when(ruleUseCase.createRule(any())).thenReturn(
            NotificationRule.create(
                "Rule 1", "Desc", "TRIP_DELAY_RECORDED",
                NotificationChannel.IN_APP, RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.WARNING
            )
        );

        ruleMvc.perform(post("/notification-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Rule 1"));
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
}
