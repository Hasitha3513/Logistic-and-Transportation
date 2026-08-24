package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class NotificationService implements NotificationUseCase {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
    }

    private Set<String> buildRecipientKeys(String username, Set<String> userRoles) {
        Set<String> keys = new HashSet<>();
        if (username != null && !username.trim().isBlank()) {
            keys.add(username.trim());
        }
        if (userRoles != null) {
            for (String role : userRoles) {
                if (role != null && !role.trim().isBlank()) {
                    String cleanRole = role.trim().toUpperCase();
                    if (!cleanRole.startsWith("ROLE_") && !cleanRole.startsWith("ROLE:")) {
                        keys.add("ROLE:" + cleanRole);
                        keys.add("ROLE_" + cleanRole);
                    } else if (cleanRole.startsWith("ROLE_")) {
                        keys.add("ROLE:" + cleanRole.substring(5));
                        keys.add(cleanRole);
                    } else {
                        keys.add(cleanRole);
                    }
                }
            }
        }
        keys.add("ALL");
        return keys;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> listNotificationsForUser(String username, Set<String> userRoles, int limit) {
        Set<String> recipients = buildRecipientKeys(username, userRoles);
        int effectiveLimit = limit > 0 ? limit : 50;
        return notificationRepository.findByRecipientsOrderByCreatedAtDesc(recipients, effectiveLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> getNotification(UUID id, String username, Set<String> userRoles) {
        Objects.requireNonNull(id, "Notification ID must not be null");
        Set<String> recipients = buildRecipientKeys(username, userRoles);
        return notificationRepository.findById(id)
            .filter(n -> recipients.contains(n.recipient()) || "ALL".equalsIgnoreCase(n.recipient()));
    }

    @Override
    public Notification markAsRead(UUID id, String username, Set<String> userRoles) {
        Notification notification = getNotification(id, username, userRoles)
            .orElseThrow(() -> new NoSuchElementException("Notification not found or access denied: " + id));

        if (notification.status() == com.transportlogistics.app.notification.domain.model.NotificationStatus.READ) {
            return notification;
        }

        Notification readNotification = notification.markRead();
        return notificationRepository.save(readNotification);
    }

    @Override
    public int markAllAsRead(String username, Set<String> userRoles) {
        Set<String> recipients = buildRecipientKeys(username, userRoles);
        return notificationRepository.markAllAsReadForRecipients(recipients);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String username, Set<String> userRoles) {
        Set<String> recipients = buildRecipientKeys(username, userRoles);
        return notificationRepository.countUnreadByRecipients(recipients);
    }
}
