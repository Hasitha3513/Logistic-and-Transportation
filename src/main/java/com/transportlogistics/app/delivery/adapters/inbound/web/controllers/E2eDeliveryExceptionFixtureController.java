package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Profile-restricted source fixture for real Delivery-exception E2E tests. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/delivery-exception-fixtures")
public class E2eDeliveryExceptionFixtureController {
    private static final UUID CUSTOMER = UUID.fromString("32000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN = UUID.fromString("33000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION = UUID.fromString("33000000-0000-0000-0000-000000000002");
    private final JdbcClient jdbc;
    private final CurrentTenant currentTenant;

    public E2eDeliveryExceptionFixtureController(JdbcClient jdbc, CurrentTenant currentTenant) {
        this.jdbc = jdbc;
        this.currentTenant = currentTenant;
    }

    @PostMapping
    @Transactional
    Fixture create() {
        UUID id = UUID.randomUUID();
        UUID tenantId = currentTenant.required().tenantId();
        String number = "DEL-2026-%06d".formatted(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.sql("""
                insert into delivery_order
                    (id, tenant_id, delivery_number, customer_id, origin_location_id, destination_location_id,
                     priority, service_type, window_start, window_end, instructions, status, version,
                     created_at, updated_at, created_by, updated_by)
                values (:id, :tenantId, :number, :customer, :origin, :destination, 'NORMAL', 'STANDARD',
                        :start, :end, 'US-78 isolated E2E source fixture', 'READY_FOR_ASSIGNMENT', 0,
                        :now, :now, 'e2e', 'e2e')
                """).param("id", id).param("tenantId", tenantId).param("number", number)
            .param("customer", CUSTOMER).param("origin", ORIGIN).param("destination", DESTINATION)
            .param("start", now).param("end", now.plusHours(2)).param("now", now).update();
        return new Fixture(id);
    }

    record Fixture(UUID id) {}
}
