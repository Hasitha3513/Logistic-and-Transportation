package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void listNotificationsForUser_queriesMatchingRecipients() {
        when(notificationRepository.findByRecipientsOrderByCreatedAtDesc(any(), anyInt()))
            .thenReturn(List.of(
                Notification.createPending(
                    UUID.randomUUID(), UUID.randomUUID(), "EVENT", NotificationChannel.IN_APP,
                    "dispatcher1", NotificationSeverity.INFO, "Title", "Msg", null
                ).markSent()
            ));

        List<Notification> list = notificationService.listNotificationsForUser("dispatcher1", Set.of("DISPATCHER"), 20);

        assertThat(list).hasSize(1);
        verify(notificationRepository).findByRecipientsOrderByCreatedAtDesc(
            argThat(recipients -> recipients.contains("dispatcher1") && recipients.contains("ROLE:DISPATCHER")),
            eq(20)
        );
    }

    @Test
    void markAsRead_marksNotificationReadAndSaves() {
        UUID id = UUID.randomUUID();
        Notification sent = Notification.createPending(
            UUID.randomUUID(), UUID.randomUUID(), "EVENT", NotificationChannel.IN_APP,
            "dispatcher1", NotificationSeverity.INFO, "Title", "Msg", null
        ).markSent();

        when(notificationRepository.findById(id)).thenReturn(Optional.of(sent));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.markAsRead(id, "dispatcher1", Set.of());

        assertThat(result.status()).isEqualTo(NotificationStatus.READ);
        assertThat(result.readAt()).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_callsRepository() {
        when(notificationRepository.markAllAsReadForRecipients(any())).thenReturn(5);

        int count = notificationService.markAllAsRead("dispatcher1", Set.of("DISPATCHER"));

        assertThat(count).isEqualTo(5);
        verify(notificationRepository).markAllAsReadForRecipients(any());
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countUnreadByRecipients(any())).thenReturn(3L);

        long count = notificationService.getUnreadCount("dispatcher1", Set.of());

        assertThat(count).isEqualTo(3L);
    }
}
