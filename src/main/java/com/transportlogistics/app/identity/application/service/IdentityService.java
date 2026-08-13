package com.transportlogistics.app.identity.application.service;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IdentityService implements IdentityUseCase {
    private final IdentityRepository repo;

    public IdentityService(IdentityRepository repo) {
        this.repo = repo;
    }

    public User createUser(User u) {
        return repo.saveUser(u);
    }

    public User getUser(UUID id) {
        return repo.findUser(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<User> listUsers() {
        return repo.findUsers();
    }

    public User updateUser(UUID id, User u) {
        getUser(id);
        return repo.saveUser(u);
    }

    public void deactivateUser(UUID id) {
        var u = getUser(id);
        repo.saveUser(new User(u.id(), u.username(), u.email(), u.passwordHash(), u.firstName(), u.lastName(), u.phone(), false, u.createdAt(), u.updatedAt()));
    }

    public Role createRole(Role r) {
        return repo.saveRole(r);
    }

    public Role getRole(UUID id) {
        return repo.findRole(id).orElseThrow(() -> new NotFoundException("Role not found: " + id));
    }

    public List<Role> listRoles() {
        return repo.findRoles();
    }

    public Role updateRole(UUID id, Role r) {
        getRole(id);
        return repo.saveRole(r);
    }

    public void deleteRole(UUID id) {
        repo.deleteRole(id);
    }

    public Optional<User> findByUsername(String username) {
        return repo.findUserByUsername(username);
    }
}
