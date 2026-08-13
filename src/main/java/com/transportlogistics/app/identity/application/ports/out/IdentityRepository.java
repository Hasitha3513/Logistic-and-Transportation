package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    User saveUser(User user);

    Optional<User> findUser(UUID id);

    Optional<User> findUserByUsername(String username);

    List<User> findUsers();

    Role saveRole(Role role);

    Optional<Role> findRole(UUID id);

    List<Role> findRoles();

    void deleteRole(UUID id);
}
