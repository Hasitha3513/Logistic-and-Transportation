package com.transportlogistics.app.notification.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationUseCase;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.CreateNotificationRuleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationSecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NotificationRuleUseCase ruleUseCase;
    @MockBean private NotificationUseCase notificationUseCase;

    @Test
    void shouldDenyUnauthenticatedAccessToRules() throws Exception {
        mvc.perform(get("/notification-rules").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void shouldDenyUnauthorizedAccessToRules() throws Exception {
        mvc.perform(get("/notification-rules").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_RULE_VIEW")
    void shouldAllowRuleListWithViewAuthority() throws Exception {
        when(ruleUseCase.listRules()).thenReturn(List.of());

        mvc.perform(get("/notification-rules").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_RULE_VIEW")
    void shouldDenyRuleCreationForViewer() throws Exception {
        CreateNotificationRuleRequest request = new CreateNotificationRuleRequest(
            "Security Rule",
            "Rule description",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.INFO
        );

        mvc.perform(post("/notification-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_RULE_MANAGE")
    void shouldAllowRuleCreationForManager() throws Exception {
        CreateNotificationRuleRequest request = new CreateNotificationRuleRequest(
            "Security Rule",
            "Rule description",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.INFO
        );

        when(ruleUseCase.createRule(any())).thenReturn(
            NotificationRule.create(
                "Security Rule", "Rule description", "TRIP_DELAY_RECORDED",
                NotificationChannel.IN_APP, RecipientType.ROLE, "DISPATCHER", true, NotificationSeverity.INFO
            )
        );

        mvc.perform(post("/notification-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDenyUnauthenticatedAccessToNotifications() throws Exception {
        mvc.perform(get("/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_VIEW")
    void shouldAllowNotificationsAccessWithNotificationViewAuthority() throws Exception {
        when(notificationUseCase.listNotificationsForUser(any(), any(), any(Integer.class))).thenReturn(List.of());
        when(notificationUseCase.getUnreadCount(any(), any())).thenReturn(0L);

        mvc.perform(get("/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/notifications/unread-count").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
