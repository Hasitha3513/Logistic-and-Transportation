package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.GeofenceManagementService;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GeofenceManagementPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("49000000-0000-0000-0000-000000000011");
    private static final UUID OTHER_TENANT = UUID.fromString("49000000-0000-0000-0000-000000000012");
    private static final UUID ACTOR = UUID.fromString("49000000-0000-0000-0000-000000000013");
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");

    @Autowired GeofenceManagementService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createIsTenantScopedPersistentIdempotentAndSafelyAudited() {
        Geofence first = service.create(context(TENANT), create("Secure Yard"), "create-key");
        Geofence replay = service.create(context(TENANT), create("Secure Yard"), "create-key");
        Geofence otherTenant = service.create(
                context(OTHER_TENANT), create("Secure Yard"), "create-key");

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(otherTenant.id()).isNotEqualTo(first.id());
        assertThat(service.get(OTHER_TENANT, first.id())).isEmpty();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=? AND action='GEOFENCE_CREATED'
                  AND safe_detail NOT LIKE '%longitude%' AND safe_detail NOT LIKE '%latitude%'
                """, Integer.class, TENANT, first.id())).isEqualTo(1);
        assertThatThrownBy(() -> service.create(
                context(TENANT), create("Different Yard"), "create-key"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void lifecycleCommandsReplaySafelyRejectStaleVersionsAndPreserveHistory() {
        Geofence draft = service.create(context(TENANT), create("Lifecycle"), "life-create");
        Geofence active = service.activate(context(TENANT), draft.id(), 0, "activate-key");
        assertThat(service.activate(context(TENANT), draft.id(), 0, "activate-key").id())
                .isEqualTo(draft.id());
        Geofence disabled = service.disable(
                context(TENANT), draft.id(), active.version(), "maintenance", "disable-key");
        Geofence reactivated = service.activate(
                context(TENANT), draft.id(), disabled.version(), "reactivate-key");
        Geofence disabledAgain = service.disable(context(TENANT), draft.id(), reactivated.version(),
                "retirement preparation", "disable-again-key");
        Geofence retired = service.retire(context(TENANT), draft.id(), disabledAgain.version(),
                "boundary removed", "retire-key");

        assertThat(retired.lifecycle()).isEqualTo(GeofenceLifecycle.RETIRED);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=? AND action IN(
                 'GEOFENCE_ACTIVATED','GEOFENCE_DISABLED','GEOFENCE_RETIRED')
                """, Integer.class, TENANT, draft.id())).isEqualTo(5);
        assertThatThrownBy(() -> service.update(context(TENANT), draft.id(), retired.version(),
                update("Changed"))).isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("GEOFENCE_LIFECYCLE_INVALID"));
        assertThatThrownBy(() -> service.disable(
                context(TENANT), draft.id(), 0, "stale", "stale-disable"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("GEOFENCE_STALE_VERSION"));
    }

    @Test
    void duplicateNameAndCrossTenantMutationAreSafelyShaped() {
        Geofence first = service.create(context(TENANT), create("Tenant Name"), "name-one");
        assertThatThrownBy(() -> service.create(
                context(TENANT), create("Tenant Name"), "name-two"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("GEOFENCE_NAME_CONFLICT"));
        assertThatThrownBy(() -> service.update(
                context(OTHER_TENANT), first.id(), first.version(), update("Intrusion")))
                .isInstanceOf(com.transportlogistics.app.shared.domain.NotFoundException.class);
    }

    @Test
    void concurrentActivationCannotExceedFiveHundredActiveDefinitions() throws Exception {
        seedActive(TENANT, 499);
        Geofence first = service.create(context(TENANT), create("Candidate A"), "candidate-a");
        Geofence second = service.create(context(TENANT), create("Candidate B"), "candidate-b");
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> activateAfter(barrier, first, "activate-a"));
            var two = executor.submit(() -> activateAfter(barrier, second, "activate-b"));
            assertThat(one.get() + two.get()).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_geofence
                WHERE tenant_id=? AND lifecycle='ACTIVE'
                """, Integer.class, TENANT)).isEqualTo(500);
    }

    @Test
    void concurrentSameIdempotencyKeyProducesOneLogicalCreateAndAudit() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> createAfter(barrier, "Concurrent", "same-key"));
            var two = executor.submit(() -> createAfter(barrier, "Concurrent", "same-key"));
            assertThat(one.get().id()).isEqualTo(two.get().id());
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_geofence WHERE tenant_id=? AND name='Concurrent'
                """, Integer.class, TENANT)).isOne();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event
                WHERE tenant_id=? AND action='GEOFENCE_CREATED'
                """, Integer.class, TENANT)).isOne();
    }

    @Test
    void concurrentSameExpectedVersionAllowsOneUpdateWithoutLostMutation() throws Exception {
        Geofence draft = service.create(context(TENANT), create("Stale Race"), "stale-create");
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> updateAfter(barrier, draft, "Winner A"));
            var two = executor.submit(() -> updateAfter(barrier, draft, "Winner B"));
            assertThat(one.get() + two.get()).isEqualTo(1);
        }
        assertThat(service.get(TENANT, draft.id()).orElseThrow().version()).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_audit_event
                WHERE tenant_id=? AND target_id=? AND action='GEOFENCE_UPDATED'
                """, Integer.class, TENANT, draft.id())).isOne();
    }

    @Test
    void queryPagesAreBoundedAndPendingMembershipIsHidden() {
        Geofence definition = service.create(context(TENANT), create("Query"), "query-create");
        assertThat(service.list(TENANT, null, null, null, 0, 100).total()).isEqualTo(1);
        assertThat(service.memberships(TENANT, null, definition.id(), 0, 100).items()).isEmpty();
        assertThatThrownBy(() -> service.list(TENANT, null, null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transitions(
                TENANT, null, null, null, null, null, null, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private int activateAfter(CyclicBarrier barrier, Geofence geofence, String key) throws Exception {
        barrier.await();
        try {
            service.activate(context(TENANT), geofence.id(), geofence.version(), key);
            return 1;
        } catch (BusinessRuleException exception) {
            assertThat(exception.code()).isEqualTo("GEOFENCE_ACTIVE_LIMIT");
            return 0;
        }
    }

    private Geofence createAfter(CyclicBarrier barrier, String name, String key) throws Exception {
        barrier.await();
        return service.create(context(TENANT), create(name), key);
    }

    private int updateAfter(CyclicBarrier barrier, Geofence geofence, String name) throws Exception {
        barrier.await();
        try {
            service.update(context(TENANT), geofence.id(), geofence.version(), update(name));
            return 1;
        } catch (BusinessRuleException exception) {
            assertThat(exception.code()).isEqualTo("GEOFENCE_STALE_VERSION");
            return 0;
        }
    }

    private void seedActive(UUID tenantId, int count) {
        jdbc.update("""
                INSERT INTO tracking_geofence(
                 id,tenant_id,name,type,polygon_vertices,min_longitude,max_longitude,min_latitude,
                 max_latitude,location_id,alert_enter_enabled,alert_exit_enabled,lifecycle,version,
                 created_at,created_by,updated_at,updated_by)
                SELECT gen_random_uuid(), ?, 'Existing ' || value, 'UNAUTHORIZED_ZONE',
                 '[{"longitude":79.8,"latitude":6.8},{"longitude":79.9,"latitude":6.8},
                   {"longitude":79.9,"latitude":6.9},{"longitude":79.8,"latitude":6.8}]'::jsonb,
                 79.8,79.9,6.8,6.9,NULL,TRUE,FALSE,'ACTIVE',1,now(),?,now(),?
                FROM generate_series(1,?) value
                """, tenantId, ACTOR, ACTOR, count);
    }

    private static GeofenceManagementUseCase.Context context(UUID tenantId) {
        return new GeofenceManagementUseCase.Context(tenantId, ACTOR, "acceptance", NOW);
    }

    private static GeofenceManagementUseCase.CreateGeofence create(String name) {
        return new GeofenceManagementUseCase.CreateGeofence(name, GeofenceType.UNAUTHORIZED_ZONE,
                polygon(), null, new GeofenceAlertPolicy(true, false));
    }

    private static GeofenceManagementUseCase.UpdateGeofence update(String name) {
        return new GeofenceManagementUseCase.UpdateGeofence(name, GeofenceType.UNAUTHORIZED_ZONE,
                polygon(), null, new GeofenceAlertPolicy(true, false));
    }

    private static GeofencePolygon polygon() {
        return GeofencePolygon.of(List.of(new Wgs84Coordinate(79.8, 6.8),
                new Wgs84Coordinate(79.9, 6.8), new Wgs84Coordinate(79.9, 6.9)));
    }
}
