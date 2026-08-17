package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
class IdentityPersistenceAdapter implements IdentityRepository {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;
    private final NamedParameterJdbcTemplate jdbc;

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
        return user(users.saveAndFlush(e));
    }

    @Transactional
    public User saveUserWithRoles(User user, Set<UUID> roleIds) {
        var saved = saveUser(user);
        replaceUserRoles(saved.id(), roleIds);
        return findUser(saved.id()).orElse(saved);
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

    @Transactional
    public Role saveRole(Role v) {
        var e = new RoleEntity();
        e.setId(v.id());
        e.setName(v.name());
        e.setDescription(v.description());
        e.setActive(v.active());
        var saved = roles.saveAndFlush(e);
        replaceRolePermissions(saved.getId(), v.permissions());
        return role(saved);
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

    @Transactional
    public void replaceUserRoles(UUID userId, Set<UUID> roleIds) {
        jdbc.update("DELETE FROM app_user_role WHERE user_id = :userId", Map.of("userId", userId));
        roleIds.forEach(roleId -> jdbc.update(
                "INSERT INTO app_user_role (user_id, role_id) VALUES (:userId, :roleId)",
                Map.of("userId", userId, "roleId", roleId)));
    }

    public Set<Role> findRolesByIds(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        var params = new MapSqlParameterSource("ids", roleIds);
        return new HashSet<>(jdbc.query("SELECT id, name, description, active FROM app_role WHERE id IN (:ids)",
                params, (rs, rowNum) -> new Role(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("description"), rs.getBoolean("active"),
                        permissions(rs.getObject("id", UUID.class)))));
    }

    private User user(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getPasswordHash(), e.getFirstName(), e.getLastName(), e.getPhone(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt(), rolesForUser(e.getId()));
    }

    private Role role(RoleEntity e) {
        return new Role(e.getId(), e.getName(), e.getDescription(), e.isActive(), permissions(e.getId()));
    }

    private Set<Role> rolesForUser(UUID userId) {
        return new HashSet<>(jdbc.query("""
                SELECT r.id, r.name, r.description, r.active
                FROM app_role r JOIN app_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = :userId
                """, Map.of("userId", userId), (rs, rowNum) -> {
            var roleId = rs.getObject("id", UUID.class);
            return new Role(roleId, rs.getString("name"), rs.getString("description"), rs.getBoolean("active"),
                    permissions(roleId));
        }));
    }

    private Set<String> permissions(UUID roleId) {
        return new HashSet<>(jdbc.query("""
                SELECT p.code FROM app_permission p
                JOIN app_role_permission rp ON rp.permission_code = p.code
                WHERE rp.role_id = :roleId AND p.active = TRUE
                """, Map.of("roleId", roleId), (rs, rowNum) -> rs.getString("code")));
    }

    private void replaceRolePermissions(UUID roleId, Set<String> requestedPermissions) {
        var permissions = requestedPermissions == null ? Set.<String>of() : Set.copyOf(requestedPermissions);
        if (!permissions.isEmpty()) {
            var params = new MapSqlParameterSource("codes", permissions);
            var count = jdbc.queryForObject("SELECT COUNT(*) FROM app_permission WHERE active = TRUE AND code IN (:codes)",
                    params, Integer.class);
            if (count == null || count != permissions.size()) {
                throw new IllegalArgumentException("One or more permissions are missing or inactive");
            }
        }
        jdbc.update("DELETE FROM app_role_permission WHERE role_id = :roleId", Map.of("roleId", roleId));
        permissions.forEach(permission -> jdbc.update(
                "INSERT INTO app_role_permission (role_id, permission_code) VALUES (:roleId, :permission)",
                Map.of("roleId", roleId, "permission", permission)));
    }
}
