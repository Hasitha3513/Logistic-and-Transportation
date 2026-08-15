package com.transportlogistics.app.identity.infrastructure.config;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("h2")
@ConditionalOnProperty(name = "app.dev.identity-bootstrap.enabled", havingValue = "true")
class LocalIdentityBootstrap implements ApplicationRunner {
    private static final Set<String> MVP_PERMISSIONS = Set.of(
            "IDENTITY_READ", "IDENTITY_MANAGE",
            "VEHICLE_VIEW", "VEHICLE_CREATE", "VEHICLE_UPDATE", "VEHICLE_STATUS_UPDATE",
            "VEHICLE_DOCUMENT_MANAGE", "VEHICLE_AVAILABILITY_VIEW",
            "DRIVER_VIEW", "DRIVER_CREATE", "DRIVER_UPDATE", "DRIVER_LICENSE_MANAGE", "DRIVER_AVAILABILITY_VIEW",
            "ROUTE_VIEW", "ROUTE_CREATE", "ROUTE_UPDATE",
            "TRIP_VIEW", "TRIP_CREATE", "TRIP_UPDATE", "TRIP_SUBMIT", "TRIP_APPROVE", "TRIP_REJECT",
            "TRIP_ASSIGN_VEHICLE", "TRIP_ASSIGN_DRIVER", "TRIP_ASSIGN_ROUTE", "TRIP_DISPATCH", "TRIP_START",
            "TRIP_COMPLETE", "TRIP_CLOSE", "TRIP_CANCEL", "REPORT_VIEW", "DASHBOARD_VIEW",
            "FUEL_ISSUE_VIEW", "FUEL_ISSUE_CREATE", "FUEL_ISSUE_UPDATE", "FUEL_ISSUE_SUBMIT",
            "FUEL_ISSUE_AUTHORIZE", "FUEL_ISSUE_ISSUE", "FUEL_ISSUE_CANCEL");

    private final IdentityUseCase identities;
    private final Environment environment;

    LocalIdentityBootstrap(IdentityUseCase identities, Environment environment) {
        this.identities = identities;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        var username = required("app.dev.identity-bootstrap.username");
        var password = required("app.dev.identity-bootstrap.password");
        if (identities.findByUsername(username).isPresent()) return;

        var role = identities.listRoles().stream().filter(candidate -> candidate.name().equals("LOCAL_MVP_ADMIN"))
                .findFirst().orElseGet(() -> identities.createRole(new Role(UUID.randomUUID(), "LOCAL_MVP_ADMIN",
                        "Opt-in local Phase 1 administrator with all MVP business permissions", true,
                        MVP_PERMISSIONS)));
        var now = OffsetDateTime.now();
        identities.createUser(new User(UUID.randomUUID(), username,
                        environment.getProperty("app.dev.identity-bootstrap.email", "local.operator@example.test"), null,
                        "Local", "Administrator", null, true, now, now, Set.of()), password, Set.of(role.id()));
    }

    private String required(String key) {
        var value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be provided when the local identity bootstrap is enabled");
        }
        return value;
    }
}
