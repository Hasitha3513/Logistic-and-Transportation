package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationRulePersistenceAdapter.class, NotificationPersistenceAdapter.class,
    NotificationTemplatePersistenceAdapter.class})
class NotificationPersistenceIntegrationTest {

    @Autowired
    private NotificationRulePersistenceAdapter ruleAdapter;

    @Autowired
    private NotificationPersistenceAdapter notificationAdapter;

    @Autowired
    private NotificationTemplatePersistenceAdapter templateAdapter;

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
}
