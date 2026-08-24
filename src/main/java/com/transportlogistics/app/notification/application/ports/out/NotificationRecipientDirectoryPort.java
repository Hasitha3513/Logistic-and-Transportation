package com.transportlogistics.app.notification.application.ports.out;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipientDirectoryPort {
    Optional<RecipientUser> findActiveUser(String username);

    boolean activeRoleExists(String roleName);

    List<RecipientUser> findActiveRoleMembers(String roleName);

    record RecipientUser(String username, String email) {
    }
}
