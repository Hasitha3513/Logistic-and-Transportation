package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class IdentityPersistenceAdapter implements IdentityRepository {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;

    IdentityPersistenceAdapter(UserJpaRepository u, RoleJpaRepository r) {
        users = u;
        roles = r;
    }

    public User saveUser(User v) {
        var e = new UserEntity();
        e.setId(v.id());
        e.setUsername(v.username());
        e.setEmail(v.email());
        e.setPasswordHash(v.passwordHash());
        e.setFirstName(v.firstName());
        e.setLastName(v.lastName());
        e.setPhone(v.phone());
        e.setActive(v.active());
        e.setCreatedAt(v.createdAt());
        e.setUpdatedAt(v.updatedAt());
        return user(users.save(e));
    }

    public Optional<User> findUser(UUID id) {
        return users.findById(id).map(this::user);
    }

    public Optional<User> findUserByUsername(String n) {
        return users.findByUsername(n).map(this::user);
    }

    public List<User> findUsers() {
        return users.findAll().stream().map(this::user).toList();
    }

    public Role saveRole(Role v) {
        var e = new RoleEntity();
        e.setId(v.id());
        e.setName(v.name());
        e.setDescription(v.description());
        e.setActive(v.active());
        return role(roles.save(e));
    }

    public Optional<Role> findRole(UUID id) {
        return roles.findById(id).map(this::role);
    }

    public List<Role> findRoles() {
        return roles.findAll().stream().map(this::role).toList();
    }

    public void deleteRole(UUID id) {
        roles.deleteById(id);
    }

    private User user(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getPasswordHash(), e.getFirstName(), e.getLastName(), e.getPhone(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private Role role(RoleEntity e) {
        return new Role(e.getId(), e.getName(), e.getDescription(), e.isActive());
    }
}
