package com.transportlogistics.app.identity.infrastructure.config;

import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.identity.NotificationRecipientDirectory;
import com.transportlogistics.app.identity.OperationalAssignmentDirectory;
import com.transportlogistics.app.identity.TenantAccessResolver;
import com.transportlogistics.app.identity.TenantMembershipManager;
import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.application.ports.out.PasswordHasher;
import com.transportlogistics.app.identity.application.ports.out.RefreshTokenStore;
import com.transportlogistics.app.identity.application.ports.out.TenantMembershipRepository;
import com.transportlogistics.app.identity.application.service.IdentityService;
import com.transportlogistics.app.identity.application.service.TenantAccessService;
import com.transportlogistics.app.identity.infrastructure.security.JwtProperties;
import com.transportlogistics.app.tenancy.TenantDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class IdentityConfig {
    @Bean
    IdentityUseCase identityUseCase(IdentityRepository repository, PasswordHasher passwords,
                                    AccessTokenService accessTokens, RefreshTokenStore refreshTokens,
                                    TenantAccessResolver tenantAccess, TenantMembershipManager tenantMemberships,
                                    JwtProperties properties, Clock clock) {
        return new IdentityService(repository, passwords, accessTokens, refreshTokens,
                tenantAccess, tenantMemberships, properties.refreshTokenTtl(), clock);
    }

    @Bean
    TenantAccessService tenantAccessService(TenantMembershipRepository memberships, TenantDirectory tenants,
                                            Clock clock) {
        return new TenantAccessService(memberships, tenants, clock);
    }

    @Bean
    AuthenticatedUserLookup authenticatedUserLookup(IdentityUseCase identities) {
        return username -> identities.findByUsername(username)
                .map(user -> new AuthenticatedUserLookup.AuthenticatedUser(user.id(), user.username()));
    }

    @Bean
    NotificationRecipientDirectory notificationRecipientDirectory(IdentityUseCase identities) {
        return new NotificationRecipientDirectory() {
            @Override
            public java.util.Optional<RecipientUser> findActiveUser(String username) {
                return identities.findByUsername(username)
                    .filter(com.transportlogistics.app.identity.domain.model.User::active)
                    .map(user -> new RecipientUser(user.username(), user.email()));
            }

            @Override
            public boolean activeRoleExists(String roleName) {
                return roleName != null && identities.listRoles().stream()
                    .anyMatch(role -> role.active() && role.name().equalsIgnoreCase(roleName.trim()));
            }

            @Override
            public java.util.List<RecipientUser> findActiveRoleMembers(String roleName) {
                if (roleName == null) {
                    return java.util.List.of();
                }
                return identities.listUsers().stream()
                    .filter(com.transportlogistics.app.identity.domain.model.User::active)
                    .filter(user -> user.hasRole(roleName.trim()))
                    .map(user -> new RecipientUser(user.username(), user.email()))
                    .toList();
            }
        };
    }

    @Bean
    OperationalAssignmentDirectory operationalAssignmentDirectory(IdentityUseCase identities) {
        return new OperationalAssignmentDirectory() {
            @Override
            public boolean eligibleUser(java.util.UUID tenantId, java.util.UUID userId, String permission) {
                try {
                    var context = new IdentityUseCase.AdministrationContext(tenantId, "operations-assignment", java.util.Set.of());
                    var user = identities.getUser(context, userId);
                    return user.active() && user.hasPermission(permission);
                } catch (com.transportlogistics.app.shared.domain.NotFoundException exception) {
                    return false;
                }
            }

            @Override
            public boolean activeRole(String roleCode) {
                return roleCode != null && identities.listRoles().stream()
                    .anyMatch(role -> role.active() && role.name().equalsIgnoreCase(roleCode.trim()));
            }
        };
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
