package com.transportlogistics.app.identity.application.ports.in;

import com.transportlogistics.app.identity.domain.model.AuthTokens;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityUseCase {
    User createUser(User user, String rawPassword, Set<UUID> roleIds);

    User getUser(UUID id);

    List<User> listUsers();

    User updateUser(UUID id, User user, String rawPassword, Set<UUID> roleIds);

    void deactivateUser(UUID id);

    Role createRole(Role role);

    Role getRole(UUID id);

    List<Role> listRoles();

    Role updateRole(UUID id, Role role);

    void deleteRole(UUID id);

    Optional<User> findByUsername(String username);

    AuthTokens login(String username, String password);

    AuthTokens refresh(String refreshToken);

    void logout(String refreshToken);

    User currentUser(String username);
}
