package com.transportlogistics.app.identity.infrastructure.adapters.in.web;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class IdentityController {
    private final IdentityUseCase useCase;

    IdentityController(IdentityUseCase u) {
        useCase = u;
    }

    @PostMapping("/auth/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        var u = useCase.findByUsername(r.username()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        return ResponseEntity.ok(new AuthResponse("development-token-" + u.id(), "development-refresh-" + u.id(), "Bearer", 3600));
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest r) {
        return ResponseEntity.ok(new AuthResponse("refreshed-token", "refreshed-token", "Bearer", 3600));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    @GetMapping("/auth/me")
    ResponseEntity<User> me() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/users")
    ResponseEntity<User> createUser(@Valid @RequestBody UserRequest r) {
        var now = OffsetDateTime.now();
        return ResponseEntity.status(201).body(useCase.createUser(new User(UUID.randomUUID(), r.username(), r.email(), r.password() == null ? "{noop}change-me" : r.password(), r.firstName(), r.lastName(), r.phone(), r.active() == null || r.active(), now, now)));
    }

    @GetMapping("/users/{id}")
    User getUser(@PathVariable UUID id) {
        return useCase.getUser(id);
    }

    @GetMapping("/users")
    List<User> listUsers() {
        return useCase.listUsers();
    }

    @PutMapping("/users/{id}")
    User updateUser(@PathVariable UUID id, @Valid @RequestBody UserRequest r) {
        var old = useCase.getUser(id);
        return useCase.updateUser(id, new User(id, r.username(), r.email(), r.password() == null ? old.passwordHash() : r.password(), r.firstName(), r.lastName(), r.phone(), r.active() == null ? old.active() : r.active(), old.createdAt(), OffsetDateTime.now()));
    }

    @DeleteMapping("/users/{id}")
    MessageResponse deactivateUser(@PathVariable UUID id) {
        useCase.deactivateUser(id);
        return new MessageResponse("User deactivated");
    }

    @PostMapping("/roles")
    ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequest r) {
        return ResponseEntity.status(201).body(useCase.createRole(new Role(UUID.randomUUID(), r.name(), r.description(), r.active() == null || r.active())));
    }

    @GetMapping("/roles")
    List<Role> listRoles() {
        return useCase.listRoles();
    }

    @GetMapping("/roles/{id}")
    Role getRole(@PathVariable UUID id) {
        return useCase.getRole(id);
    }

    @PutMapping("/roles/{id}")
    Role updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest r) {
        return useCase.updateRole(id, new Role(id, r.name(), r.description(), r.active() == null || r.active()));
    }

    @DeleteMapping("/roles/{id}")
    MessageResponse deleteRole(@PathVariable UUID id) {
        useCase.deleteRole(id);
        return new MessageResponse("Role deleted");
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    record MessageResponse(String message) {
    }

    record UserRequest(@NotBlank String username, @Email @NotBlank String email, String password,
                       @NotBlank String firstName, @NotBlank String lastName, String phone, Boolean active) {
    }

    record RoleRequest(@NotBlank String name, String description, Boolean active) {
    }
}
