package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryAnalyticsUseCase;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.CanonicalTenant;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
@EnabledIf("postgresAvailable")
class DeliveryAnalyticsPersistencePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {

    private static boolean postgresAvailable() {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            return true;
        }
        String url = System.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/transport_integration");
        String user = System.getProperty("DB_USERNAME", "transport_app");
        String pass = System.getProperty("DB_PASSWORD", "LocalDb-Transport-2026");
        try (var conn = java.sql.DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final UUID TENANT_A = CanonicalTenant.ID;
    private static final UUID TENANT_B = UUID.fromString("7c3e44b7-68dc-4bcb-a53c-f1a8d5df0da2");

    private static final TenantExecutionContext TENANT_A_CONTEXT = new TenantExecutionContext(TENANT_A, UUID.randomUUID(), "admin", "corr-a");
    private static final TenantExecutionContext TENANT_B_CONTEXT = new TenantExecutionContext(TENANT_B, UUID.randomUUID(), "admin", "corr-b");

    @Autowired private Flyway flyway;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private TenantContextExecutor contexts;
    @Autowired private DeliveryAnalyticsUseCase analyticsService;

    private final UUID locColombo = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID locKandy = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID locGalle = UUID.fromString("20000000-0000-0000-0000-000000000003");

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();

        jdbc.update("""
                INSERT INTO tenant (tenant_id, tenant_code, tenant_name, default_currency, default_time_zone, status, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, 'tenant-b', 'Tenant B', 'LKR', 'Asia/Colombo', 'ACTIVE', NOW(), 'system', NOW(), 'system', 0)
                ON CONFLICT (tenant_id) DO NOTHING
                """, TENANT_B);
    }

    @Test
    @DisplayName("Golden Dataset Verification on Real PostgreSQL: verifies KPI math, punctuality, attempts, RTO and Tenant isolation")
    void verifyGoldenDatasetAndTenantIsolation() {
        OffsetDateTime baseTime = OffsetDateTime.of(2026, 8, 15, 8, 0, 0, 0, ZoneOffset.UTC);

        // Seed Tenant A golden dataset:
        // Order 1 (DELIVERED, On-Time, 0 failed attempts)
        UUID order1 = UUID.randomUUID();
        insertOrder(TENANT_A, order1, "DEL-A01", locColombo, locKandy, "DELIVERED", baseTime, baseTime.plusHours(4));
        insertPod(TENANT_A, order1, baseTime.plusHours(3)); // On-time: completed 3h <= 4h window end

        // Order 2 (DELIVERED, Late by 30 mins, 1 failed attempt, redelivered)
        UUID order2 = UUID.randomUUID();
        insertOrder(TENANT_A, order2, "DEL-A02", locColombo, locKandy, "DELIVERED", baseTime, baseTime.plusHours(4));
        UUID attempt2 = UUID.randomUUID();
        insertAttempt(TENANT_A, order2, attempt2, 1, baseTime.plusHours(3), "CUSTOMER_UNAVAILABLE", "REDELIVERY_ELIGIBLE");
        UUID sched2 = UUID.randomUUID();
        insertSchedule(TENANT_A, order2, attempt2, sched2, baseTime.plusDays(1).plusHours(2), baseTime.plusDays(1).plusHours(4), "CONFIRMED");
        insertPod(TENANT_A, order2, baseTime.plusDays(1).plusHours(4).plusMinutes(30)); // Late: completed 4:30 > 4:00 window end

        // Order 3 (RETURN_TO_BASE, 2 failed attempts)
        UUID order3 = UUID.randomUUID();
        insertOrder(TENANT_A, order3, "DEL-A03", locColombo, locGalle, "RETURN_TO_BASE", baseTime, baseTime.plusHours(6));
        UUID attempt3_1 = UUID.randomUUID();
        insertAttempt(TENANT_A, order3, attempt3_1, 1, baseTime.plusHours(3), "CUSTOMER_REFUSED", "RETURN_TO_BASE_REQUIRED");
        UUID attempt3_2 = UUID.randomUUID();
        insertAttempt(TENANT_A, order3, attempt3_2, 2, baseTime.plusHours(5), "CUSTOMER_REFUSED", "RETURN_TO_BASE_REQUIRED");

        // Order 4 (FAILED_ATTEMPT active, 1 failed attempt)
        UUID order4 = UUID.randomUUID();
        insertOrder(TENANT_A, order4, "DEL-A04", locColombo, locKandy, "FAILED_ATTEMPT", baseTime, baseTime.plusHours(8));
        UUID attempt4 = UUID.randomUUID();
        insertAttempt(TENANT_A, order4, attempt4, 1, baseTime.plusHours(6), "ACCESS_RESTRICTED", "REDELIVERY_ELIGIBLE");

        // Order 5 (READY_FOR_ASSIGNMENT active)
        UUID order5 = UUID.randomUUID();
        insertOrder(TENANT_A, order5, "DEL-A05", locColombo, locGalle, "READY_FOR_ASSIGNMENT", baseTime, baseTime.plusHours(8));

        // Seed Tenant B dataset (1 delivered order)
        UUID orderB1 = UUID.randomUUID();
        insertOrder(TENANT_B, orderB1, "DEL-B01", locColombo, locKandy, "DELIVERED", baseTime, baseTime.plusHours(2));
        insertPod(TENANT_B, orderB1, baseTime.plusHours(1));

        // Execute analytics under Tenant A
        var criteria = new DeliveryAnalyticsCriteria(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null, null, null
        );

        AtomicReference<DeliveryAnalyticsSummary> summaryARef = new AtomicReference<>();
        contexts.within(TENANT_A_CONTEXT, () -> {
            summaryARef.set(analyticsService.getSummary(criteria));
        });
        DeliveryAnalyticsSummary summaryA = summaryARef.get();

        // Tenant A assertions:
        // Total orders: 5 (order1, order2, order3, order4, order5)
        assertThat(summaryA.totalOrders()).isEqualTo(5);
        // Active orders: 2 (order4 in FAILED_ATTEMPT, order5 in READY_FOR_ASSIGNMENT)
        assertThat(summaryA.activeOrders()).isEqualTo(2);
        // Terminal completed orders: 3 (order1 delivered, order2 delivered, order3 returned to base)
        assertThat(summaryA.terminalCompletedOrders()).isEqualTo(3);
        assertThat(summaryA.deliveredOrders()).isEqualTo(2);
        assertThat(summaryA.returnedToBaseOrders()).isEqualTo(1);

        // Order success rate: 2 / 3 * 100 = 66.67%
        assertThat(summaryA.orderSuccessRate()).isEqualTo(BigDecimal.valueOf(66.67));

        // First attempt success rate: 1 / 2 * 100 = 50.00% (order1 had 0 failed attempts; order2 had 1)
        assertThat(summaryA.firstAttemptSuccessRate()).isEqualTo(BigDecimal.valueOf(50.00).setScale(2));

        // Punctuality: order1 is on-time, order2 is late by 30 mins -> onTime = 1, late = 1
        assertThat(summaryA.onTimeDeliveredOrders()).isEqualTo(1);
        assertThat(summaryA.lateDeliveredOrders()).isEqualTo(1);
        assertThat(summaryA.onTimeDeliveryRate()).isEqualTo(BigDecimal.valueOf(50.00).setScale(2));
        assertThat(summaryA.lateDeliveryRate()).isEqualTo(BigDecimal.valueOf(50.00).setScale(2));
        assertThat(summaryA.averageDelayMinutes()).isEqualTo(BigDecimal.valueOf(30.0).setScale(1));

        // Failed attempts: 1 (order2) + 2 (order3) + 1 (order4) = 4 total failed attempts in Tenant A
        assertThat(summaryA.totalFailedAttempts()).isEqualTo(4);
        assertThat(summaryA.averageFailedAttemptsPerOrder()).isEqualTo(BigDecimal.valueOf(0.80).setScale(2));

        // Redelivery rate: 1 order redelivered (order2) out of 5 = 20.00%
        assertThat(summaryA.redeliveredOrders()).isEqualTo(1);
        assertThat(summaryA.redeliveryRate()).isEqualTo(BigDecimal.valueOf(20.00).setScale(2));
        // Redelivery success rate: 1 redelivered order delivered out of 1 completed = 100.00%
        assertThat(summaryA.redeliverySuccessRate()).isEqualTo(BigDecimal.valueOf(100.00).setScale(2));

        // RTO rate: 1 RTO / 3 terminal = 33.33%
        assertThat(summaryA.returnToBaseRate()).isEqualTo(BigDecimal.valueOf(33.33));

        // Execute analytics under Tenant B
        AtomicReference<DeliveryAnalyticsSummary> summaryBRef = new AtomicReference<>();
        contexts.within(TENANT_B_CONTEXT, () -> {
            summaryBRef.set(analyticsService.getSummary(criteria));
        });
        DeliveryAnalyticsSummary summaryB = summaryBRef.get();

        // Tenant B assertions: exactly 1 order, 100% success, completely isolated from Tenant A
        assertThat(summaryB.totalOrders()).isEqualTo(1);
        assertThat(summaryB.deliveredOrders()).isEqualTo(1);
        assertThat(summaryB.orderSuccessRate()).isEqualTo(BigDecimal.valueOf(100.00).setScale(2));
        assertThat(summaryB.onTimeDeliveryRate()).isEqualTo(BigDecimal.valueOf(100.00).setScale(2));
        assertThat(summaryB.totalFailedAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Empty dataset returns 200 OK with null rates and zero counts")
    void verifyEmptyDataset() {
        var criteria = new DeliveryAnalyticsCriteria(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null, null, null
        );

        AtomicReference<DeliveryAnalyticsSummary> summaryRef = new AtomicReference<>();
        contexts.within(TENANT_A_CONTEXT, () -> {
            summaryRef.set(analyticsService.getSummary(criteria));
        });
        DeliveryAnalyticsSummary summary = summaryRef.get();

        assertThat(summary.totalOrders()).isEqualTo(0);
        assertThat(summary.deliveredOrders()).isEqualTo(0);
        assertThat(summary.orderSuccessRate()).isNull();
        assertThat(summary.onTimeDeliveryRate()).isNull();
        assertThat(summary.averageDelayMinutes()).isNull();
        assertThat(summary.redeliverySuccessRate()).isNull();
    }

    private void insertOrder(UUID tenantId, UUID orderId, String deliveryNumber, UUID originLoc, UUID destLoc,
                             String status, OffsetDateTime windowStart, OffsetDateTime windowEnd) {
        jdbc.update("""
                INSERT INTO delivery_order (
                    id, tenant_id, delivery_number, customer_id, origin_location_id, destination_location_id,
                    priority, service_type, window_start, window_end, instructions, status, version,
                    created_at, updated_at, created_by, updated_by
                ) VALUES (
                    ?, ?, ?, '10000000-0000-0000-0000-000000000001', ?, ?,
                    'NORMAL', 'STANDARD', ?, ?, 'test instructions', ?, 0,
                    ?, ?, 'test-user', 'test-user'
                )
                """, orderId, tenantId, deliveryNumber, originLoc, destLoc, windowStart, windowEnd, status, windowStart, windowStart);
    }

    private void insertPod(UUID tenantId, UUID orderId, OffsetDateTime acceptedAt) {
        UUID podId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO proof_of_delivery (
                    id, tenant_id, delivery_order_id, status, accepted_at, accepted_by, version,
                    created_at, updated_at, created_by, updated_by
                ) VALUES (
                    ?, ?, ?, 'FINALIZED', ?, 'driver.test', 0,
                    ?, ?, 'driver.test', 'driver.test'
                )
                """, podId, tenantId, orderId, acceptedAt, acceptedAt, acceptedAt);
    }

    private void insertAttempt(UUID tenantId, UUID orderId, UUID attemptId, int attemptNumber,
                               OffsetDateTime attemptTime, String reason, String disposition) {
        jdbc.update("""
                INSERT INTO delivery_attempt (
                    id, tenant_id, delivery_id, attempt_number, attempt_timestamp, failure_reason,
                    notes, disposition, recorded_by, recorded_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, 'failed attempt', ?, 'driver.test', ?
                )
                """, attemptId, tenantId, orderId, attemptNumber, attemptTime, reason, disposition, attemptTime);
    }

    private void insertSchedule(UUID tenantId, UUID orderId, UUID attemptId, UUID scheduleId,
                                OffsetDateTime start, OffsetDateTime end, String status) {
        jdbc.update("""
                INSERT INTO delivery_redelivery_schedule (
                    id, tenant_id, delivery_order_id, delivery_attempt_id, scheduling_method,
                    scheduled_start_time, scheduled_end_time, status, scheduled_by, scheduled_at,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, 'AGENT_ASSISTED',
                    ?, ?, ?, 'dispatcher.test', ?,
                    ?, ?
                )
                """, scheduleId, tenantId, orderId, attemptId, start, end, status, start, start, start);
    }
}
