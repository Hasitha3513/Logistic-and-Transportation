package com.transportlogistics.app.fleet.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class DriverPayrollV71PostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("46000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("46000000-0000-0000-0000-000000000002");
    @Autowired JdbcTemplate jdbc;

    @Test
    void v71HasExactTenantOwnedCommandStructureAndNoForeignModuleForeignKeys() {
        assertThat(jdbc.queryForObject("select version from flyway_schema_history where success "
            + "order by installed_rank desc limit 1", String.class)).isEqualTo("83");
        assertThat(jdbc.queryForList("select column_name from information_schema.columns "
            + "where table_schema='public' and table_name='driver_payroll_worker_mapping_command' "
            + "and is_nullable='NO'", String.class)).containsExactlyInAnyOrder(
                "id", "tenant_id", "driver_id", "idempotency_key", "request_hash", "result_mapping_id",
                "result_version", "result_external_system_alias", "result_external_worker_reference",
                "result_active", "result_updated_by", "result_created_at", "result_updated_at", "actor_id",
                "created_at");
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname='public' "
            + "and tablename='driver_payroll_worker_mapping_command' and indexdef like '%(tenant_id,%'",
            Integer.class)).isGreaterThanOrEqualTo(3);
        assertThat(jdbc.queryForList("select distinct ccu.table_name from information_schema.table_constraints tc "
            + "join information_schema.constraint_column_usage ccu on ccu.constraint_name=tc.constraint_name "
            + "and ccu.constraint_schema=tc.constraint_schema where tc.table_schema='public' "
            + "and tc.table_name='driver_payroll_worker_mapping_command' and tc.constraint_type='FOREIGN KEY'",
            String.class)).containsExactly("driver_payroll_worker_mapping");
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname='public' "
            + "and tablename='driver_payroll_worker_mapping_command' "
            + "and indexdef like 'CREATE UNIQUE INDEX% (tenant_id, idempotency_key)'", Integer.class)).isEqualTo(1);
    }

    @Test
    void commandIdentityIsTenantLeadingAndAppendOnly() {
        UUID driverA = insertMapping(TENANT_A, "A");
        UUID driverB = insertMapping(TENANT_B, "B");
        UUID commandA = insertCommand(TENANT_A, driverA, "same-key");
        UUID commandB = insertCommand(TENANT_B, driverB, "same-key");

        assertThat(jdbc.queryForObject("select count(*) from driver_payroll_worker_mapping_command "
            + "where idempotency_key='same-key'", Integer.class)).isEqualTo(2);
        assertThat(commandA).isNotEqualTo(commandB);
        assertThatThrownBy(() -> jdbc.update("update driver_payroll_worker_mapping_command "
            + "set request_hash=? where id=?", "b".repeat(64), commandA)).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from driver_payroll_worker_mapping_command where id=?",
            commandA)).isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    void releasedLinesAndHistoryAreDatabaseImmutable() {
        UUID batch = insertBatch(TENANT_A, "APPROVED", "released");
        UUID line = insertLine(TENANT_A, batch, UUID.randomUUID(), UUID.randomUUID());
        UUID history = UUID.randomUUID();
        jdbc.update("insert into driver_payroll_input_history(id,tenant_id,batch_id,action,from_state,to_state,"
            + "actor_id,detail,created_at) values(?,?,?,'BATCH_APPROVED','VALIDATED','APPROVED',?,null,?)",
            history, TENANT_A, batch, UUID.randomUUID(), OffsetDateTime.now());

        assertThatThrownBy(() -> jdbc.update("update driver_payroll_input_line set amount=2 where id=?", line))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("released payroll lines are immutable");
        assertThatThrownBy(() -> jdbc.update("delete from driver_payroll_input_line where id=?", line))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("released payroll lines are immutable");
        assertThatThrownBy(() -> jdbc.update("update driver_payroll_input_history set detail='changed' where id=?",
            history)).isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from driver_payroll_input_history where id=?", history))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    void correctionForeignTenantOriginalLineIsRejectedByCompositeForeignKey() {
        UUID tenantBOriginal = insertBatch(TENANT_B, "EXPORTED", "tenant-b-original");
        UUID tenantBOriginalLine = insertLine(TENANT_B, tenantBOriginal, UUID.randomUUID(), UUID.randomUUID());
        UUID tenantAOriginal = insertBatch(TENANT_A, "EXPORTED", "tenant-a-original");
        UUID tenantACorrection = insertCorrectionBatch(TENANT_A, tenantAOriginal, "tenant-a-correction");
        assertThatThrownBy(() -> insertCorrectionLine(TENANT_A, tenantACorrection, tenantBOriginalLine))
            .isInstanceOf(DataAccessException.class);
    }

    private UUID insertMapping(UUID tenant, String suffix) {
        UUID id = UUID.randomUUID(); UUID driver = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("insert into driver_payroll_worker_mapping(id,tenant_id,driver_id,external_system_alias,"
            + "external_worker_reference,active,version,updated_by,created_at,updated_at) "
            + "values(?,?,?,'PAYROLL',?,true,0,?,?,?)", id, tenant, driver, "WORKER-" + suffix,
            UUID.randomUUID(), now, now);
        return driver;
    }

    private UUID insertCommand(UUID tenant, UUID driver, String key) {
        UUID mapping = jdbc.queryForObject("select id from driver_payroll_worker_mapping where tenant_id=? "
            + "and driver_id=?", UUID.class, tenant, driver);
        UUID id = UUID.randomUUID(); UUID actor = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("insert into driver_payroll_worker_mapping_command(id,tenant_id,driver_id,idempotency_key,"
            + "request_hash,result_mapping_id,result_version,result_external_system_alias,"
            + "result_external_worker_reference,result_active,result_updated_by,result_created_at,result_updated_at,"
            + "actor_id,created_at) values(?,?,?,?,?,?,0,'PAYROLL','WORKER',true,?,?,?,?,?)", id, tenant, driver,
            key, "a".repeat(64), mapping, actor, now, now, actor, now);
        return id;
    }

    private UUID insertBatch(UUID tenant, String lifecycle, String key) {
        UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("insert into driver_payroll_input_batch(id,tenant_id,batch_type,period_start,"
            + "period_end_exclusive,cutoff_at,currency,lifecycle,prepared_by,validation_hash,idempotency_key,"
            + "version,created_at,updated_at) values(?,?,'REGULAR',current_date-1,current_date+1,?,'LKR',?,?,?, ?,0,?,?)",
            id, tenant, now, lifecycle, UUID.randomUUID(), "a".repeat(64), key, now, now);
        return id;
    }

    private UUID insertCorrectionBatch(UUID tenant, UUID original, String key) {
        UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("insert into driver_payroll_input_batch(id,tenant_id,batch_type,correction_of_batch_id,"
            + "period_start,period_end_exclusive,cutoff_at,currency,lifecycle,prepared_by,idempotency_key,version,"
            + "created_at,updated_at) values(?,?,'CORRECTION',?,current_date-1,current_date+1,?,'LKR','DRAFT',"
            + "?,?,0,?,?)", id, tenant, original, now, UUID.randomUUID(), key, now, now);
        return id;
    }

    private UUID insertLine(UUID tenant, UUID batch, UUID driver, UUID trip) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into driver_payroll_input_line(id,tenant_id,batch_id,driver_id,trip_id,trip_number,"
            + "category,reason_code,description,quantity,unit,rate,amount,source_snapshot_hash,"
            + "external_worker_reference) values(?,?,?, ?,?,'TRIP','TRIP_EARNING','RATE','source',1,'TRIP',1,1,?,"
            + "'WORKER')", id, tenant, batch, driver, trip, "a".repeat(64));
        return id;
    }

    private void insertCorrectionLine(UUID tenant, UUID batch, UUID originalLine) {
        jdbc.update("insert into driver_payroll_input_line(id,tenant_id,batch_id,driver_id,trip_id,trip_number,"
            + "category,reason_code,description,quantity,unit,rate,amount,original_line_id) values(?,?,?, ?,?,"
            + "'TRIP','DEDUCTION','CORRECTION','delta',1,'FIXED',1,1,?)", UUID.randomUUID(), tenant, batch,
            UUID.randomUUID(), UUID.randomUUID(), originalLine);
    }
}
