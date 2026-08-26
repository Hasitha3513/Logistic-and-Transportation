package com.transportlogistics.app.identity;

import java.util.List;
import java.util.Optional;

/** Public identity directory for notification recipient resolution. */
public interface NotificationRecipientDirectory {
    Optional<RecipientUser> findActiveUser(String username);

    boolean activeRoleExists(String roleName);

    List<RecipientUser> findActiveRoleMembers(String roleName);

    record RecipientUser(String username, String email) {
    }
}
