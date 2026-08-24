package com.transportlogistics.app.notification.infrastructure.adapters.out.identity;

import com.transportlogistics.app.identity.NotificationRecipientDirectory;
import com.transportlogistics.app.notification.application.ports.out.NotificationRecipientDirectoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class IdentityNotificationRecipientDirectoryAdapter implements NotificationRecipientDirectoryPort {
    private final NotificationRecipientDirectory directory;

    IdentityNotificationRecipientDirectoryAdapter(NotificationRecipientDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Optional<RecipientUser> findActiveUser(String username) {
        return directory.findActiveUser(username).map(this::map);
    }

    @Override
    public boolean activeRoleExists(String roleName) {
        return directory.activeRoleExists(roleName);
    }

    @Override
    public List<RecipientUser> findActiveRoleMembers(String roleName) {
        return directory.findActiveRoleMembers(roleName).stream().map(this::map).toList();
    }

    private RecipientUser map(NotificationRecipientDirectory.RecipientUser user) {
        return new RecipientUser(user.username(), user.email());
    }
}
