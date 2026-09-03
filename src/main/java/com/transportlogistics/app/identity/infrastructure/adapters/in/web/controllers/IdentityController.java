package com.transportlogistics.app.identity.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request.LoginRequest;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request.RefreshTokenRequest;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request.RoleRequest;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request.UserRequest;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.AuthResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.MessageResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.RoleResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.UserResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.mappers.IdentityWebMapper;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.validation.Valid;
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
    private final IdentityWebMapper mapper;
    private final CurrentTenant currentTenant;

    @PostMapping("/auth/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        return ResponseEntity.ok(mapper.toResponse(useCase.login(r.username(), r.password())));
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest r) {
        return ResponseEntity.ok(mapper.toResponse(useCase.refresh(r.refreshToken())));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        useCase.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    @GetMapping("/auth/me")
    UserResponse me(Authentication authentication) {
        return mapper.toResponse(useCase.currentUser(authentication.getName()));
    }

    @PostMapping("/users")
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest r, Authentication authentication) {
        var now = OffsetDateTime.now();
        var user = new User(UUID.randomUUID(), r.username(), r.email(), null, r.firstName(), r.lastName(), r.phone(),
                r.active() == null || r.active(), now, now, Set.of());
        return ResponseEntity.status(201).body(mapper.toResponse(useCase.createUser(context(authentication), user,
                r.password(), r.roleIds())));
    }

    @GetMapping("/users/{id}")
    UserResponse getUser(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(useCase.getUser(context(authentication), id));
    }

    @GetMapping("/users")
    List<UserResponse> listUsers(Authentication authentication) {
        return mapper.toUserResponseList(useCase.listUsers(context(authentication)));
    }

    @PutMapping("/users/{id}")
    UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UserRequest r,
                            Authentication authentication) {
        var context = context(authentication);
        var old = useCase.getUser(context, id);
        var updated = new User(id, r.username(), r.email(), null, r.firstName(), r.lastName(), r.phone(),
                r.active() == null ? old.active() : r.active(), old.createdAt(), OffsetDateTime.now(), old.roles());
        return mapper.toResponse(useCase.updateUser(context, id, updated, r.password(), r.roleIds()));
    }

    @DeleteMapping("/users/{id}")
    MessageResponse deactivateUser(@PathVariable UUID id, Authentication authentication) {
        useCase.deactivateUser(context(authentication), id);
        return new MessageResponse("User deactivated");
    }

    @PostMapping("/roles")
    ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest r, Authentication authentication) {
        var created = useCase.createRole(context(authentication), new Role(UUID.randomUUID(), r.name(), r.description(),
                r.active() == null || r.active(), r.permissions()));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/roles")
    List<RoleResponse> listRoles() {
        return mapper.toRoleResponseList(useCase.listRoles());
    }

    @GetMapping("/roles/{id}")
    RoleResponse getRole(@PathVariable UUID id) {
        return mapper.toResponse(useCase.getRole(id));
    }

    @PutMapping("/roles/{id}")
    RoleResponse updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest r,
                            Authentication authentication) {
        var updated = useCase.updateRole(context(authentication), id,
                new Role(id, r.name(), r.description(), r.active() == null || r.active(), r.permissions()));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/roles/{id}")
    MessageResponse deleteRole(@PathVariable UUID id, Authentication authentication) {
        useCase.deleteRole(context(authentication), id);
        return new MessageResponse("Role deleted");
    }

    private IdentityUseCase.AdministrationContext context(Authentication authentication) {
        var tenant = currentTenant.required();
        var permissions = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                .filter(authority -> !authority.startsWith("ROLE_")).collect(java.util.stream.Collectors.toSet());
        return new IdentityUseCase.AdministrationContext(tenant.tenantId(), tenant.username(), permissions);
    }
}
