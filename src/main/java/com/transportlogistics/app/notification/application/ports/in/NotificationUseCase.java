package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NotificationUseCase {
    List<Notification> listNotificationsForUser(String username, Set<String> userRoles, int limit);
    Optional<Notification> getNotification(UUID id, String username, Set<String> userRoles);
    Notification markAsRead(UUID id, String username, Set<String> userRoles);
    int markAllAsRead(String username, Set<String> userRoles);
    long getUnreadCount(String username, Set<String> userRoles);
}
