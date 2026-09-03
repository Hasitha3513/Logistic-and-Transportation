package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityRepository {
    User saveUser(User user);

    User saveUserWithRoles(User user, Set<UUID> roleIds);

    Optional<User> findUser(UUID id);

    Optional<User> findUserByUsername(String username);

    List<User> findUsers();

    Optional<User> findUser(UUID id, UUID tenantId);

    List<User> findUsers(UUID tenantId);

    void replaceUserRoles(UUID userId, Set<UUID> roleIds);

    Set<Role> findRolesByIds(Set<UUID> roleIds);

    Role saveRole(Role role);

    Optional<Role> findRole(UUID id);

    List<Role> findRoles();

    void deleteRole(UUID id);

    boolean roleAssignedOutsideTenant(UUID roleId, UUID tenantId);
}
