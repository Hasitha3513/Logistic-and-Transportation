package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.GpsExceptionService;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

class Us55V99V100PostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private final JdbcTemplate jdbc; private final Flyway flyway; private final GpsExceptionService service;
    @Autowired Us55V99V100PostgreSqlAcceptanceTest(DataSource ds,Flyway flyway,GpsExceptionService service){this.jdbc=new JdbcTemplate(ds);this.flyway=flyway;this.service=service;}
    @Test void currentHeadSeedsOnlyApprovedRolesAndCreatesDurableCommandTable(){
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("104");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_permission WHERE code IN ('GPS_EXCEPTION_VIEW','GPS_EXCEPTION_REVIEW') AND active",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_role WHERE name IN ('ADMIN','LOCAL_MVP_ADMIN','DISPATCHER')",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_name='tracking_gps_exception_acknowledgement_command'",Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname IN ('uq_tracking_gps_exception_ack_command','fk_tracking_gps_exception_ack_episode','ck_tracking_gps_exception_ack_key','ck_tracking_gps_exception_ack_fingerprint','ck_tracking_gps_exception_ack_version','ck_tracking_gps_exception_ack_response')",Integer.class)).isEqualTo(6);
    }

    @Test void v98AndV99UpgradePathsPreserveRoleDataAndRepeatedStartupIsIdempotent() {
        flyway.clean();
        Flyway.configure().configuration(flyway.getConfiguration()).target("98").load().migrate();
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES (?,?,?,true),(?,?,?,true),(?,?,?,true)",
                UUID.randomUUID(),"ADMIN","Admin",UUID.randomUUID(),"LOCAL_MVP_ADMIN","Local admin",
                UUID.randomUUID(),"DISPATCHER","Dispatcher");
        int rolesAtV98=jdbc.queryForObject("SELECT count(*) FROM app_role",Integer.class);
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("104");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_role",Integer.class)).isEqualTo(rolesAtV98);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_role_permission rp JOIN app_role r ON r.id=rp.role_id WHERE rp.permission_code IN ('GPS_EXCEPTION_VIEW','GPS_EXCEPTION_REVIEW') AND r.name IN ('ADMIN','LOCAL_MVP_ADMIN','DISPATCHER')",Integer.class)).isEqualTo(6);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        flyway.clean();
        Flyway.configure().configuration(flyway.getConfiguration()).target("99").load().migrate();
        assertThat(jdbc.queryForObject("SELECT to_regclass('tracking_gps_exception_acknowledgement_command')",String.class)).isNull();
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("104");
    }

    @Test void concurrentRetryDifferentTenantAndStoredSnapshotAreDurable() throws Exception {
        UUID tenant=UUID.randomUUID(),actor=UUID.randomUUID(),episode=episode(tenant,"concurrent");
        var context=context(tenant,actor);String key="concurrent-replay-key-001";
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        Callable<GpsExceptionUseCase.Acknowledgement> request=()->{ready.countDown();start.await();return service.acknowledge(context,episode,0," reviewed ",key);};
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
            var first=pool.submit(request);var second=pool.submit(request);ready.await();start.countDown();
            assertThat(second.get()).isEqualTo(first.get());
        }
        assertThat(count("tracking_gps_exception_acknowledgement_command",tenant)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND action='GPS_EXCEPTION_ACKNOWLEDGED'",Integer.class,tenant)).isOne();
        assertThat(count("notification",tenant)).isZero();
        assertThat(count("operational_exception_case",tenant)).isZero();

        var original=service.acknowledge(context,episode,0,"reviewed",key);
        jdbc.update("UPDATE tracking_gps_exception_episode SET status='RESOLVED',resolved_at=last_observed_at,version=version+1 WHERE tenant_id=? AND id=?",tenant,episode);
        assertThat(service.acknowledge(context,episode,0,"reviewed",key)).isEqualTo(original);

        UUID otherTenant=UUID.randomUUID(),otherEpisode=episode(otherTenant,"other-tenant");
        assertThat(service.acknowledge(context(otherTenant,UUID.randomUUID()),otherEpisode,0,"reviewed",key).status().name()).isEqualTo("ACKNOWLEDGED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_gps_exception_acknowledgement_command WHERE idempotency_key=?",Integer.class,key)).isEqualTo(2);
    }

    @Test void conflictingActorStaleLifecycleAndTenantForeignKeyFailClosed() {
        UUID tenant=UUID.randomUUID(),actor=UUID.randomUUID(),episode=episode(tenant,"conflict");String key="conflict-replay-key-001";
        service.acknowledge(context(tenant,actor),episode,0,"reviewed",key);
        assertThatThrownBy(()->service.acknowledge(context(tenant,UUID.randomUUID()),episode,0,"reviewed",key)).isInstanceOf(RuntimeException.class);
        UUID open=episode(tenant,"stale");
        assertThatThrownBy(()->service.acknowledge(context(tenant,actor),open,9,"reviewed","stale-version-key-001")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO tracking_gps_exception_acknowledgement_command(id,tenant_id,idempotency_key,request_fingerprint,actor_id,episode_id,expected_version,response_snapshot,completed_at) VALUES(?,?,?,?,?,?,0,'{}'::jsonb,now())",UUID.randomUUID(),UUID.randomUUID(),"foreign-key-test-001","a".repeat(64),actor,episode)).isInstanceOf(RuntimeException.class);
    }

    @Test void auditFailureRollsBackEpisodeAndCompletedCommand() {
        UUID tenant=UUID.randomUUID(),actor=UUID.randomUUID(),episode=episode(tenant,"rollback");
        jdbc.execute("CREATE FUNCTION fail_gps_ack_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action='GPS_EXCEPTION_ACKNOWLEDGED' THEN RAISE EXCEPTION 'forced audit failure'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER fail_gps_ack_audit BEFORE INSERT ON tracking_audit_event FOR EACH ROW EXECUTE FUNCTION fail_gps_ack_audit()");
        try {
            assertThatThrownBy(()->service.acknowledge(context(tenant,actor),episode,0,"reviewed","rollback-audit-key-001")).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT status FROM tracking_gps_exception_episode WHERE tenant_id=? AND id=?",String.class,tenant,episode)).isEqualTo("OPEN");
            assertThat(count("tracking_gps_exception_acknowledgement_command",tenant)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND action='GPS_EXCEPTION_ACKNOWLEDGED'",Integer.class,tenant)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER fail_gps_ack_audit ON tracking_audit_event");
            jdbc.execute("DROP FUNCTION fail_gps_ack_audit()");
        }
    }

    private GpsExceptionUseCase.Context context(UUID tenant,UUID actor){return new GpsExceptionUseCase.Context(tenant,actor,"gps-pg-test",Instant.parse("2026-09-17T10:00:00Z"));}
    private int count(String table,UUID tenant){return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE tenant_id=?",Integer.class,tenant);}
    private UUID episode(UUID tenant,String suffix){UUID device=UUID.randomUUID(),episode=UUID.randomUUID();Instant now=Instant.parse("2026-09-17T09:00:00Z");
        jdbc.update("INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,registered_by,version,created_at,updated_at) VALUES(?,?,?,'GENERIC','ACTIVE',?,?,0,?,?)",device,tenant,"v100-"+suffix,Timestamp.from(now),UUID.randomUUID(),Timestamp.from(now),Timestamp.from(now));
        jdbc.update("INSERT INTO tracking_gps_exception_episode(id,tenant_id,tracking_device_id,exception_type,severity,status,opened_at,last_observed_at,evidence_count,consecutive_recovery_points,version) VALUES(?,?,?,'PROCESSING_FAILURE','WARNING','OPEN',?,?,1,0,0)",episode,tenant,device,Timestamp.from(now),Timestamp.from(now));return episode;}
}
