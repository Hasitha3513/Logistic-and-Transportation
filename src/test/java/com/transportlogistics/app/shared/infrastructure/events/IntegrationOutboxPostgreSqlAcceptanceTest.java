package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationOutboxPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DurableEventPublisher publisher;
    @Autowired DurableEventWorker worker;
    @Autowired TenantContextExecutor tenantContexts;
    @Autowired TransactionTemplate transactions;

    @Test
    void currentHeadV62RetainsV60TenantQualifiedPollingAndTerminalIndexes() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("62");
        var definitions = jdbc.queryForList("select indexdef from pg_indexes where schemaname='public' "
            + "and tablename='integration_outbox_event'", String.class);
        assertThat(definitions).anyMatch(value -> value.contains("tenant_id, status, next_attempt_at"));
        assertThat(definitions).anyMatch(value -> value.contains("tenant_id, status, locked_until"));
        assertThat(definitions).anyMatch(value -> value.contains("tenant_id, event_id, consumer_name"));
        assertThat(definitions).anyMatch(value -> value.contains("tenant_id, status, updated_at"));
    }

    @Test
    void publicationIsAtomicWithCallerTransactionAndLogicalReplayIsDeduplicated() {
        UUID tenantId = UUID.randomUUID();
        var context = new TenantExecutionContext(tenantId, UUID.randomUUID(), "outbox-test", "outbox-test");
        var committed = event(tenantId);
        tenantContexts.within(context, () -> transactions.executeWithoutResult(status -> publisher.publish(committed)));
        tenantContexts.within(context, () -> transactions.executeWithoutResult(status -> publisher.publish(committed)));

        assertThat(count(committed)).isEqualTo(1);
        String storedPayload = jdbc.queryForObject("select payload::text from integration_outbox_event "
            + "where tenant_id=? and event_id=? and consumer_name=?", String.class,
            committed.tenantId(), committed.eventId(), committed.durableConsumer());
        assertThat(storedPayload.toLowerCase()).doesNotContain("access_token", "magic", "token_hash", "access_code",
            "authorization", "password", "provider", "signature", "photo", "medical");

        var rolledBack = event(tenantId);
        assertThatThrownBy(() -> tenantContexts.within(context, () -> transactions.executeWithoutResult(status -> {
            publisher.publish(rolledBack);
            throw new IllegalStateException("business rollback");
        }))).isInstanceOf(IllegalStateException.class);
        assertThat(count(rolledBack)).isZero();
    }

    @Test
    void staleExhaustedClaimsAreTerminalizedOnlyInsideTheirExplicitTenantContext() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID eventA = insertStaleClaim(tenantA);
        UUID eventB = insertStaleClaim(tenantB);
        var contextA = new TenantExecutionContext(tenantA, UUID.randomUUID(), "outbox-worker", "tenant-a");

        tenantContexts.within(contextA, worker::processDue);

        assertThat(status(eventA)).isEqualTo("FAILED");
        assertThat(status(eventB)).isEqualTo("PROCESSING");
    }

    private int count(DeliveryCustomerNotificationEvent event) {
        return jdbc.queryForObject("select count(*) from integration_outbox_event "
            + "where tenant_id=? and event_id=? and consumer_name=?", Integer.class,
            event.tenantId(), event.eventId(), event.durableConsumer());
    }

    private DeliveryCustomerNotificationEvent event(UUID tenantId) {
        return new DeliveryCustomerNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED", tenantId,
            OffsetDateTime.parse("2026-09-03T10:00:00Z"), 1, "DELIVERY_ORDER", UUID.randomUUID(),
            Map.of("customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-P1-01", "actor", "test",
                "status", "DELIVERED", "completedAt", "2026-09-03T09:59:00Z"));
    }

    private UUID insertStaleClaim(UUID tenantId) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
            insert into integration_outbox_event(id,event_id,tenant_id,consumer_name,event_type,event_version,
             aggregate_type,aggregate_id,payload,occurred_at,status,attempt_count,next_attempt_at,locked_until,
             created_at,updated_at,row_version)
            values(?,?,?,?,?,1,?,?,?::jsonb,?,'PROCESSING',5,?,?,?,?,0)
            """, id, UUID.randomUUID(), tenantId, "missing-handler", "TEST_EVENT", "TEST", UUID.randomUUID(),
            "{}", now.minusMinutes(10), now.minusMinutes(10), now.minusMinutes(1), now.minusMinutes(10),
            now.minusMinutes(1));
        return id;
    }

    private String status(UUID id) {
        return jdbc.queryForObject("select status from integration_outbox_event where id=?", String.class, id);
    }
}
