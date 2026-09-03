package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.integration.IntegrationPlatformProbeEvent;
import com.transportlogistics.app.integration.domain.model.IntegrationMapping;
import com.transportlogistics.app.integration.ports.inbound.IntegrationExchangeUseCase;
import com.transportlogistics.app.integration.ports.inbound.IntegrationManagementUseCase;
import com.transportlogistics.app.integration.ports.outbound.IntegrationExchangeRepository;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
    "app.integration.exchange.enabled=false",
    "app.integration.file.controlled-sandbox-root=target/us73-postgres-sandbox"
})
class IntegrationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("73000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("73000000-0000-0000-0000-000000000002");
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired ObjectMapper json;
    @Autowired IntegrationManagementUseCase management;
    @Autowired IntegrationExchangeUseCase exchanges;
    @Autowired IntegrationExchangeRepository exchangeRepository;
    @Autowired TenantContextExecutor tenantContexts;

    @Test
    void v61IsCurrentAndOwnsExactlyFiveIntegrationTablesWhileReusingSharedOutbox() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("61");
        assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname='public' "
            + "and tablename like 'integration_%' order by tablename", String.class))
            .contains("integration_configuration", "integration_mapping", "integration_exchange",
                "integration_exchange_attempt", "integration_audit_event", "integration_outbox_event")
            .doesNotContain("integration_external_outbox", "integration_outbox");
    }

    @Test
    void tenantNameAndMappingVersionUniquenessAreDatabaseEnforced() throws Exception {
        Fixture first = fixture(TENANT_A, "SANDBOX", "DRAFT");
        assertThatThrownBy(() -> insertConfiguration(UUID.randomUUID(), TENANT_A, "Sandbox", "SANDBOX", "DRAFT"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertMapping(UUID.randomUUID(), TENANT_A, first.configurationId(),
            first.mapping(), 1)).isInstanceOf(DataIntegrityViolationException.class);
        fixture(TENANT_B, "SANDBOX", "DRAFT");
    }

    @Test
    void durableAcceptanceIsIdempotentAndCrossTenantReadsUseSafeNotFound() throws Exception {
        Fixture fixture = fixture(TENANT_A, "ACTIVE SANDBOX", "ACTIVE");
        UUID eventId = UUID.randomUUID();
        var fact = new IntegrationExchangeUseCase.ProbeFact(eventId, TENANT_A, fixture.configurationId(),
            IntegrationPlatformProbeEvent.EVENT_TYPE, 1, "INTEGRATION_CONFIGURATION", now(),
            Map.of("probeId", UUID.randomUUID().toString(), "probeType", "CONTROLLED_SANDBOX", "sequence", 1L));
        within(TENANT_A, () -> {
            for (int replay = 0; replay < 25; replay++) exchanges.acceptProbe(fact);
        });
        assertThat(jdbc.queryForObject("select count(*) from integration_exchange where tenant_id=? and source_event_id=?",
            Integer.class, TENANT_A, eventId)).isEqualTo(1);

        var tenantB = new IntegrationManagementUseCase.Context(TENANT_B, "tenant-b", "cross-tenant");
        assertThatThrownBy(() -> within(TENANT_B, () -> management.get(tenantB, fixture.configurationId())))
            .isInstanceOf(NotFoundException.class)
            .extracting("code").isEqualTo("INTEGRATION_NOT_FOUND");
    }

    @Test
    void attemptOrderingAndTenantConsistentForeignKeysAreDatabaseEnforced() throws Exception {
        Fixture fixture = fixture(TENANT_A, "ORDERING", "ACTIVE");
        UUID exchangeId = insertExchange(fixture, UUID.randomUUID());
        insertAttempt(UUID.randomUUID(), TENANT_A, exchangeId, 1);
        assertThatThrownBy(() -> insertAttempt(UUID.randomUUID(), TENANT_A, exchangeId, 1))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAttempt(UUID.randomUUID(), TENANT_B, exchangeId, 2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expiredClaimIsRecoveredWithinTenantAndPermanentIntegrityFaultBecomesTerminal() throws Exception {
        Fixture fixture = fixture(TENANT_A, "RECOVERY", "ACTIVE");
        UUID exchangeId = insertExchange(fixture, UUID.randomUUID());
        jdbc.update("update integration_exchange set status='IN_PROGRESS',attempt_count=4,locked_until=? where id=?",
            now().minusMinutes(1), exchangeId);
        java.nio.file.Path root = java.nio.file.Path.of("target/us73-postgres-sandbox");
        java.nio.file.Files.createDirectories(root);
        java.nio.file.Files.writeString(root.resolve(exchangeId + ".json"), "tampered");

        within(TENANT_A, () -> exchanges.processDue(TENANT_A));

        assertThat(jdbc.queryForObject("select status from integration_exchange where id=?", String.class, exchangeId))
            .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select attempt_count from integration_exchange where id=?", Integer.class,
            exchangeId)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from integration_exchange_attempt where exchange_id=?",
            Integer.class, exchangeId)).isEqualTo(1);
        java.nio.file.Files.deleteIfExists(root.resolve(exchangeId + ".json"));
    }

    @Test
    void dueClaimBatchIsCappedAtFifty() throws Exception {
        Fixture fixture = fixture(TENANT_A, "BATCH CAP", "ACTIVE");
        for (int row = 0; row < 51; row++) insertExchange(fixture, UUID.randomUUID());

        assertThat(jdbc.queryForObject("select count(*) from integration_exchange e "
            + "join integration_configuration c on c.id=e.configuration_id and c.tenant_id=e.tenant_id "
            + "where e.tenant_id=? and e.status='PENDING' and c.lifecycle='ACTIVE'",
            Integer.class, TENANT_A)).isEqualTo(51);
        OffsetDateTime claimAt = now().plusDays(1);
        assertThat(jdbc.queryForObject("select count(*) from integration_exchange e "
            + "join integration_configuration c on c.id=e.configuration_id and c.tenant_id=e.tenant_id "
            + "where e.tenant_id=? and e.status='PENDING' and e.attempt_count < 5 "
            + "and e.next_attempt_at <= ? and c.lifecycle='ACTIVE'", Integer.class, TENANT_A, claimAt)).isEqualTo(51);
        assertThat(within(TENANT_A, () -> exchangeRepository.claimDue(TENANT_A, claimAt, 100))).hasSize(50);
        assertThat(jdbc.queryForObject("select count(*) from integration_exchange where tenant_id=? and status='PENDING'",
            Integer.class, TENANT_A)).isEqualTo(1);
    }

    private Fixture fixture(UUID tenantId, String name, String lifecycle) throws Exception {
        UUID configurationId = UUID.randomUUID(); UUID mappingId = UUID.randomUUID();
        insertConfiguration(configurationId, tenantId, name, name, lifecycle);
        var mapping = IntegrationMapping.active(tenantId, configurationId, "US73_PLATFORM_PROBE", 1,
            IntegrationMapping.PROBE_CONTRACT, 1, IntegrationMapping.PROBE_SCHEMA, 1, rules(), now(), "test");
        insertMapping(mappingId, tenantId, configurationId, mapping, 1);
        jdbc.update("update integration_configuration set current_mapping_id=? where id=?", mappingId,
            configurationId);
        return new Fixture(configurationId, mappingId, mapping);
    }

    private void insertConfiguration(UUID id, UUID tenant, String name, String normalized, String lifecycle) {
        jdbc.update("""
            insert into integration_configuration(id,tenant_id,name,normalized_name,integration_type,protocol,
             direction,endpoint_alias,data_classification,retry_policy,lifecycle,health,last_tested_at,
             last_tested_version,version,created_at,created_by,updated_at,updated_by)
            values(?,?,?,?,'FILE_EXCHANGE','FILE_JSON_V1','OUTBOUND','CONTROLLED_SANDBOX',
             'INTERNAL_OPERATIONAL_NON_SENSITIVE','US73_BOUNDED_V1',?,'HEALTHY',?,0,0,?,'test',?,'test')
            """, id, tenant, name, normalized, lifecycle, now(), now(), now());
    }

    private void insertMapping(UUID id, UUID tenant, UUID configurationId, IntegrationMapping mapping,
                               int version) throws Exception {
        jdbc.update("""
            insert into integration_mapping(id,tenant_id,configuration_id,mapping_key,mapping_version,
             source_contract,source_version,target_schema,target_version,rules,definition_hash,lifecycle,created_at,created_by)
            values(?,?,?,?,?,'US73_PLATFORM_PROBE',1,'US73_FILE_PROBE',1,?::jsonb,?,'ACTIVE',?,'test')
            """, id, tenant, configurationId, mapping.mappingKey(), version,
            json.writeValueAsString(mapping.rules()), mapping.definitionHash(), now());
    }

    private UUID insertExchange(Fixture fixture, UUID eventId) {
        UUID id = UUID.randomUUID(); String hash = "0".repeat(64);
        jdbc.update("""
            insert into integration_exchange(id,tenant_id,configuration_id,source_event_id,source_event_type,
             mapping_version_id,mapping_definition_hash,canonical_payload,payload_hash,status,attempt_count,
             next_attempt_at,created_at,updated_at,version)
            values(?,?,?,?,?,?,?,'{}'::jsonb,?,'PENDING',0,?,?,?,0)
            """, id, TENANT_A, fixture.configurationId(), eventId, IntegrationPlatformProbeEvent.EVENT_TYPE,
            fixture.mappingId(), fixture.mapping().definitionHash(), hash, now(), now(), now());
        return id;
    }

    private void insertAttempt(UUID id, UUID tenant, UUID exchangeId, int number) {
        jdbc.update("""
            insert into integration_exchange_attempt(id,tenant_id,exchange_id,attempt_number,started_at,
             completed_at,latency_ms,outcome) values(?,?,?,?,?,?,0,'SUCCEEDED')
            """, id, tenant, exchangeId, number, now(), now());
    }

    private List<IntegrationMapping.Rule> rules() {
        return List.of(new IntegrationMapping.Rule("probeId", "probe_id", null, IntegrationMapping.Format.UUID, false, true),
            new IntegrationMapping.Rule("probeType", "probe_type", null, IntegrationMapping.Format.ENUM, false, true),
            new IntegrationMapping.Rule("sequence", "sequence", null, IntegrationMapping.Format.DECIMAL, false, true));
    }

    private void within(UUID tenant, Runnable action) {
        tenantContexts.within(new TenantExecutionContext(tenant, UUID.randomUUID(), "acceptance", "us73"), action);
    }

    private <T> T within(UUID tenant, java.util.function.Supplier<T> action) {
        return tenantContexts.within(new TenantExecutionContext(tenant, UUID.randomUUID(), "acceptance", "us73"),
            action);
    }

    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
    private record Fixture(UUID configurationId, UUID mappingId, IntegrationMapping mapping) {}
}
