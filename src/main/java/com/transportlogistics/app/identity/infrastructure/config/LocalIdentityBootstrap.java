package com.transportlogistics.app.identity.infrastructure.config;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.TenantMembershipManager;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.transportlogistics.app.tenancy.CanonicalTenant;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Component
@Order(1)
@Profile({"h2", "docker", "postgres"})
@ConditionalOnProperty(name = "app.dev.identity-bootstrap.enabled", havingValue = "true")
class LocalIdentityBootstrap implements ApplicationRunner {
    private static final Set<String> MVP_PERMISSIONS = Set.of(
            "IDENTITY_READ", "IDENTITY_MANAGE",
            "VEHICLE_VIEW", "VEHICLE_CREATE", "VEHICLE_UPDATE", "VEHICLE_STATUS_UPDATE",
            "VEHICLE_DOCUMENT_MANAGE", "VEHICLE_AVAILABILITY_VIEW",
            "DRIVER_VIEW", "DRIVER_CREATE", "DRIVER_UPDATE", "DRIVER_LICENSE_MANAGE", "DRIVER_AVAILABILITY_VIEW",
            "ROUTE_VIEW", "ROUTE_CREATE", "ROUTE_UPDATE", "ROUTE_DISRUPTION_MANAGE",
            "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_UPDATE",
            "DEPARTMENT_VIEW", "DEPARTMENT_CREATE", "DEPARTMENT_UPDATE",
            "LOCATION_VIEW", "LOCATION_CREATE", "LOCATION_UPDATE",
            "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_UPDATE",
            "TRIP_VIEW", "TRIP_CREATE", "TRIP_UPDATE", "TRIP_SUBMIT", "TRIP_APPROVE", "TRIP_REJECT",
            "TRIP_ASSIGN_VEHICLE", "TRIP_ASSIGN_DRIVER", "TRIP_ASSIGN_ROUTE", "TRIP_DISPATCH", "TRIP_START",
            "TRIP_COMPLETE", "TRIP_CLOSE", "TRIP_CANCEL", "REPORT_VIEW", "DASHBOARD_VIEW",
            "NOTIFICATION_VIEW", "NOTIFICATION_RULE_VIEW", "NOTIFICATION_RULE_MANAGE",
            "FUEL_ISSUE_VIEW", "FUEL_ISSUE_CREATE", "FUEL_ISSUE_UPDATE", "FUEL_ISSUE_SUBMIT",
            "FUEL_ISSUE_AUTHORIZE", "FUEL_ISSUE_ISSUE", "FUEL_ISSUE_CANCEL",
            "FUEL_PURCHASE_VIEW", "FUEL_PURCHASE_CREATE", "FUEL_PURCHASE_UPDATE", "FUEL_PURCHASE_SUBMIT",
            "FUEL_PURCHASE_APPROVE", "FUEL_PURCHASE_RECEIVE", "FUEL_PURCHASE_RECONCILE", "FUEL_PURCHASE_CANCEL",
            "FUEL_PRICE_VIEW", "FUEL_PRICE_MANAGE",
            "FUEL_COST_VIEW",
            "FREIGHT_ORDER_VIEW", "FREIGHT_ORDER_MANAGE",
            "CARGO_MANIFEST_VIEW", "CARGO_MANIFEST_MANAGE", "CARGO_MANIFEST_FINALIZE",
            "LOAD_PLAN_VIEW", "LOAD_PLAN_MANAGE",
            "CARGO_INSURANCE_VIEW", "CARGO_INSURANCE_MANAGE",
            "CARGO_EXCEPTION_VIEW", "CARGO_EXCEPTION_MANAGE",
            "BUNKER_VIEW", "BUNKER_CREATE", "BUNKER_UPDATE", "BUNKER_LEDGER_VIEW", "BUNKER_DIP_RECORD", "BUNKER_ADJUST", "BUNKER_TRANSFER",
            "VEHICLE_READING_VIEW", "VEHICLE_READING_CREATE", "VEHICLE_READING_CORRECT", "VEHICLE_READING_RESET_METER",
            "VEHICLE_MAINTENANCE_MANAGE", "DRIVER_EXCEPTION_MANAGE", "DRIVER_VIOLATION_MANAGE",
            "DRIVER_MEDICAL_VIEW", "DRIVER_MEDICAL_MANAGE",
            "DRIVER_DRUG_TEST_VIEW", "DRIVER_DRUG_TEST_MANAGE",
            "LUBRICANT_LOG_VIEW", "LUBRICANT_LOG_MANAGE",
            "TRIP_LOG_VIEW", "TRIP_LOG_MANAGE",
            "DELIVERY_VIEW", "DELIVERY_CREATE", "DELIVERY_UPDATE", "DELIVERY_ASSIGN",
            "DELIVERY_POD_CAPTURE", "DELIVERY_POD_VIEW",
            "DELIVERY_FAIL_RECORD", "DELIVERY_FAIL_VIEW", "DELIVERY_FAIL_ESCALATE", "DELIVERY_RETURN_INITIATE",
            "DELIVERY_REDELIVERY_SCHEDULE", "DELIVERY_REDELIVERY_VIEW"
    );

    private final IdentityUseCase identities;
    private final Environment environment;
    private final TenantMembershipManager memberships;

    LocalIdentityBootstrap(IdentityUseCase identities, Environment environment, TenantMembershipManager memberships) {
        this.identities = identities;
        this.environment = environment;
        this.memberships = memberships;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        var username = required("app.dev.identity-bootstrap.username");
        var password = required("app.dev.identity-bootstrap.password");
        var existingUser = identities.findByUsername(username);

        var role = identities.listRoles().stream().filter(candidate -> candidate.name().equals("LOCAL_MVP_ADMIN"))
                .findFirst().map(existing -> identities.updateRole(existing.id(), new Role(existing.id(), existing.name(),
                        "Opt-in local administrator with all implemented business permissions", true, MVP_PERMISSIONS)))
                .orElseGet(() -> identities.createRole(new Role(UUID.randomUUID(), "LOCAL_MVP_ADMIN",
                        "Opt-in local administrator with all implemented business permissions", true,
                        MVP_PERMISSIONS)));
        if (existingUser.isPresent()) {
            identities.updateUser(existingUser.get().id(), existingUser.get(), password, Set.of(role.id()));
            memberships.ensureActiveMembership(existingUser.get().id(), CanonicalTenant.ID, username);
            return;
        }
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
