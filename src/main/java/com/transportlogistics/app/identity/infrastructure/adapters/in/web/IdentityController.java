package com.transportlogistics.app.identity.infrastructure.adapters.in.web;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class IdentityController {
    private final IdentityUseCase useCase;

    @PostMapping("/auth/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        return ResponseEntity.ok(AuthResponse.from(useCase.login(r.username(), r.password())));
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest r) {
        return ResponseEntity.ok(AuthResponse.from(useCase.refresh(r.refreshToken())));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        useCase.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    @GetMapping("/auth/me")
    UserResponse me(Authentication authentication) {
        return UserResponse.from(useCase.currentUser(authentication.getName()));
    }

    @PostMapping("/users")
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest r) {
        var now = OffsetDateTime.now();
        var user = new User(UUID.randomUUID(), r.username(), r.email(), null, r.firstName(), r.lastName(), r.phone(),
                r.active() == null || r.active(), now, now, Set.of());
        return ResponseEntity.status(201).body(UserResponse.from(useCase.createUser(user, r.password(), r.roleIds())));
    }

    @GetMapping("/users/{id}")
    UserResponse getUser(@PathVariable UUID id) {
        return UserResponse.from(useCase.getUser(id));
    }

    @GetMapping("/users")
    List<UserResponse> listUsers() {
        return useCase.listUsers().stream().map(UserResponse::from).toList();
    }

    @PutMapping("/users/{id}")
    UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UserRequest r) {
        var old = useCase.getUser(id);
        var updated = new User(id, r.username(), r.email(), null, r.firstName(), r.lastName(), r.phone(),
                r.active() == null ? old.active() : r.active(), old.createdAt(), OffsetDateTime.now(), old.roles());
        return UserResponse.from(useCase.updateUser(id, updated, r.password(), r.roleIds()));
    }

    @DeleteMapping("/users/{id}")
    MessageResponse deactivateUser(@PathVariable UUID id) {
        useCase.deactivateUser(id);
        return new MessageResponse("User deactivated");
    }

    @PostMapping("/roles")
    ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequest r) {
        return ResponseEntity.status(201).body(useCase.createRole(new Role(UUID.randomUUID(), r.name(), r.description(),
                r.active() == null || r.active(), r.permissions())));
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
        return useCase.updateRole(id, new Role(id, r.name(), r.description(), r.active() == null || r.active(), r.permissions()));
    }

    @DeleteMapping("/roles/{id}")
    MessageResponse deleteRole(@PathVariable UUID id) {
        useCase.deleteRole(id);
        return new MessageResponse("Role deleted");
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
        @Override
        public String toString() {
            return "LoginRequest[username=" + username + ", password=***]";
        }
    }

    record RefreshTokenRequest(@NotBlank String refreshToken) {
        @Override
        public String toString() {
            return "RefreshTokenRequest[refreshToken=***]";
        }
    }

    record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        static AuthResponse from(com.transportlogistics.app.identity.domain.model.AuthTokens tokens) {
            return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresIn());
        }

        @Override
        public String toString() {
            return "AuthResponse[accessToken=***, refreshToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
        }
    }

    record MessageResponse(String message) {
    }

    record UserRequest(@NotBlank String username, @Email @NotBlank String email, @Size(min = 12) String password,
                       @NotBlank String firstName, @NotBlank String lastName, String phone, Boolean active,
                       Set<UUID> roleIds) {
        @Override
        public String toString() {
            return "UserRequest[username=" + username + ", email=" + email + ", password=***, firstName=" + firstName +
                    ", lastName=" + lastName + ", phone=" + phone + ", active=" + active + ", roleIds=" + roleIds + "]";
        }
    }

    record RoleRequest(@NotBlank String name, String description, Boolean active, Set<@NotBlank String> permissions) {
    }

    record UserResponse(UUID id, String username, String email, String firstName, String lastName, String phone,
                        boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt, Set<String> roles,
                        Set<String> permissions) {
        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.username(), user.email(), user.firstName(), user.lastName(),
                    user.phone(), user.active(), user.createdAt(), user.updatedAt(), user.roleNames(), user.permissions());
        }
    }
}
