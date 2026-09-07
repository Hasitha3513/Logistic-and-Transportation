package com.transportlogistics.app.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.billing.application.TransportBillingService;
import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.Compliance;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.Lifecycle;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.LineCategory;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.Source.SourceType;
import com.transportlogistics.app.billing.ports.inbound.TransportBillingUseCase;
import com.transportlogistics.app.billing.ports.outbound.BillingCompliancePort;
import com.transportlogistics.app.billing.ports.outbound.BillingIntegrationPort;
import com.transportlogistics.app.billing.ports.outbound.BillingSourcePort;
import com.transportlogistics.app.billing.ports.outbound.BillingStore;
import com.transportlogistics.app.billing.ports.outbound.BillingTransaction;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tenancy.TenantDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TransportBillingConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT_A = UUID.fromString("4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a");
    private static final UUID TENANT_B = UUID.fromString("8b7d6c5a-4e3f-4210-9876-1234567890ab");
    private static final UUID PREPARER = UUID.fromString("47000000-0000-0000-0000-000000000011");
    private static final UUID APPROVER = UUID.fromString("47000000-0000-0000-0000-000000000012");
    private static final UUID CUSTOMER = UUID.fromString("47000000-0000-0000-0000-000000000020");
    private static final UUID CONFIGURATION = UUID.fromString("47000000-0000-0000-0000-000000000099");
    private static final OffsetDateTime SOURCE_COMPLETED_AT = OffsetDateTime.parse("2026-08-01T00:00:00Z");

    @Autowired BillingStore store;
    @Autowired BillingTransaction transaction;
    @Autowired BillingIntegrationPort durableIntegration;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired JdbcTemplate jdbc;
    private TransportBillingService service;

    @BeforeEach
    void setUpService() {
        BillingSourcePort sources = mock(BillingSourcePort.class);
        when(sources.find(any(), any(), any())).thenAnswer(call -> {
            UUID tenant = call.getArgument(0); SourceType type = call.getArgument(1); UUID source = call.getArgument(2);
            return Optional.of(new BillingSourcePort.SourceFact(type, source, "SOURCE-" + source,
                "CLOSED", SOURCE_COMPLETED_AT, CUSTOMER, 1));
        });
        when(sources.customerActive(any(), any())).thenReturn(true);
        BillingCompliancePort compliance = mock(BillingCompliancePort.class);
        when(compliance.decision(any(), any())).thenReturn(Compliance.NOT_REQUIRED);
        TenantDirectory tenants = mock(TenantDirectory.class);
        when(tenants.findTenant(any())).thenAnswer(call -> Optional.of(new TenantDirectory.TenantView(
            call.getArgument(0), "TEST", "Test Tenant", "LKR", "UTC", "ACTIVE")));
        BillingIntegrationPort integration = new BillingIntegrationPort() {
            @Override public Optional<UUID> activeConfiguration(UUID tenantId) { return Optional.of(CONFIGURATION); }
            @Override public void publish(TransportBillingExportRequestedV1 event) { durableIntegration.publish(event); }
        };
        service = new TransportBillingService(store, sources, compliance, integration, transaction, tenants, json, clock);
    }

    @Test
    void RACE_01_DUPLICATE_SOURCE() throws Exception {
        UUID source = UUID.randomUUID();
        var results = race(() -> service.create(preparer(TENANT_A), create(source), key("source-a")),
            () -> service.create(preparer(TENANT_A), create(source), key("source-b")));
        assertOneWinner(results, BusinessRuleException.class, ConflictException.class);
        assertThat(count("transport_billing_source_claim", "source_id", source)).isEqualTo(1);
        assertThat(count("transport_billing_record", "source_id", source)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from transport_billing_history where tenant_id=? and action='CREATED'",
            Integer.class, TENANT_A)).isEqualTo(1);
    }

    @Test
    void RACE_02_IDEMPOTENT_CREATE() throws Exception {
        UUID source = UUID.randomUUID(); String key = key("same-create");
        var results = race(() -> service.create(preparer(TENANT_A), create(source), key),
            () -> service.create(preparer(TENANT_A), create(source), key));
        assertThat(results).allSatisfy(result -> assertThat(result.failure()).isNull());
        assertThat(results.get(0).value().id()).isEqualTo(results.get(1).value().id());
        assertThat(count("transport_billing_record", "create_idempotency_key", key)).isEqualTo(1);
        assertThat(history(results.get(0).value().id(), "CREATED")).isEqualTo(1);

        // The same textual key and source UUID belong to a separate lock/idempotency namespace in Tenant B.
        var tenantB = service.create(preparer(TENANT_B), create(source), key);
        assertThat(tenantB.tenantId()).isEqualTo(TENANT_B);
        assertThat(jdbc.queryForObject("select count(*) from transport_billing_record where source_id=?",
            Integer.class, source)).isEqualTo(2);
    }

    @Test
    void RACE_03_IDEMPOTENCY_CONFLICT() throws Exception {
        String key = key("different-create");
        var results = race(() -> service.create(preparer(TENANT_A), create(UUID.randomUUID()), key),
            () -> service.create(preparer(TENANT_A), create(UUID.randomUUID()), key));
        assertOneWinner(results, ConflictException.class);
        assertThat(count("transport_billing_record", "create_idempotency_key", key)).isEqualTo(1);
    }

    @Test
    void RACE_04_EDIT_VS_APPROVE() throws Exception {
        var record = validated(key("edit-approve"));
        var results = race(() -> service.replace(preparer(TENANT_A), record.id(), record.version(), replacement("125.00")),
            () -> service.approve(approver(TENANT_A), record.id(), record.version(), key("approve")));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        var persisted = service.get(TENANT_A, record.id());
        if (persisted.lifecycle() == Lifecycle.APPROVED) {
            assertThat(persisted.totals().totalAmount()).isEqualByComparingTo("100.00");
            assertThat(persisted.validationHash()).isEqualTo(record.validationHash());
        } else {
            assertThat(persisted.lifecycle()).isEqualTo(Lifecycle.DRAFT);
            assertThat(persisted.totals().totalAmount()).isEqualByComparingTo("125.00");
            assertThat(persisted.validationHash()).isNull();
        }
    }

    @Test
    void RACE_05_DOUBLE_APPROVE() throws Exception {
        var record = validated(key("double-approve"));
        var results = race(() -> service.approve(approver(TENANT_A), record.id(), record.version(), key("approve-a")),
            () -> service.approve(new TransportBillingUseCase.Context(TENANT_A, UUID.randomUUID(), "approver-b"),
                record.id(), record.version(), key("approve-b")));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(service.get(TENANT_A, record.id()).lifecycle()).isEqualTo(Lifecycle.APPROVED);
        assertThat(history(record.id(), "APPROVED")).isEqualTo(1);
    }

    @Test
    void RACE_06_DOUBLE_FINALIZE() throws Exception {
        var record = approved(key("double-finalize"));
        var results = race(() -> service.finalizeRecord(preparer(TENANT_A), record.id(), record.version(), key("final-a")),
            () -> service.finalizeRecord(preparer(TENANT_A), record.id(), record.version(), key("final-b")));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(service.get(TENANT_A, record.id()).lifecycle()).isEqualTo(Lifecycle.FINALIZED);
        assertThat(history(record.id(), "FINALIZED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(distinct billing_number)=count(*) from transport_billing_record where tenant_id=?",
            Boolean.class, TENANT_A)).isTrue();
    }

    @Test
    void RACE_07_FINALIZE_VS_CANCEL_REVERSAL() throws Exception {
        var record = approved(key("final-cancel"));
        var results = race(() -> service.finalizeRecord(preparer(TENANT_A), record.id(), record.version(), key("final")),
            () -> service.cancel(preparer(TENANT_A), record.id(), record.version(), "cancelled", key("cancel")));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        var persisted = service.get(TENANT_A, record.id());
        assertThat(persisted.lifecycle()).isIn(Lifecycle.FINALIZED, Lifecycle.CANCELLED);
        assertThat(history(record.id(), "FINALIZED") + history(record.id(), "CANCELLED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from transport_billing_record where tenant_id=? "
            + "and original_billing_record_id=?", Integer.class, TENANT_A, record.id())).isZero();
    }

    @Test
    void RACE_08_DOUBLE_REVERSAL() throws Exception {
        var original = finalized(key("double-reverse"));
        var before = original.lines(); var total = original.totals().totalAmount();
        var results = race(() -> service.reverse(preparer(TENANT_A), original.id(), original.version(), "reverse-a", key("reverse-a")),
            () -> service.reverse(preparer(TENANT_A), original.id(), original.version(), "reverse-b", key("reverse-b")));
        assertOneWinner(results, BusinessRuleException.class, ConflictException.class);
        UUID reversal = results.stream().filter(Result::succeeded).findFirst().orElseThrow().value().id();
        var draft = service.get(TENANT_A, reversal);
        service.validate(preparer(TENANT_A), draft.id(), draft.version());
        var validated = service.get(TENANT_A, draft.id());
        var approved = service.approve(approver(TENANT_A), validated.id(), validated.version(), key("reverse-approve"));
        service.finalizeRecord(preparer(TENANT_A), approved.id(), approved.version(), key("reverse-finalize"));
        assertThat(jdbc.queryForObject("select count(*) from transport_billing_record where tenant_id=? "
            + "and original_billing_record_id=? and lifecycle<>'CANCELLED'", Integer.class, TENANT_A, original.id())).isEqualTo(1);
        var persistedOriginal = service.get(TENANT_A, original.id());
        assertThat(persistedOriginal.lifecycle()).isEqualTo(Lifecycle.REVERSED);
        assertThat(persistedOriginal.lines()).isEqualTo(before);
        assertThat(persistedOriginal.totals().totalAmount()).isEqualByComparingTo(total);
    }

    @Test
    void RACE_09_DOUBLE_EXPORT() throws Exception {
        var record = finalized(key("double-export"));
        var results = race(() -> service.export(preparer(TENANT_A), record.id(), record.version(), key("export-a")),
            () -> service.export(preparer(TENANT_A), record.id(), record.version(), key("export-b")));
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        var persisted = service.get(TENANT_A, record.id());
        assertThat(persisted.lifecycle()).isEqualTo(Lifecycle.EXPORT_REQUESTED);
        assertThat(persisted.exportEventId()).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from integration_outbox_event where tenant_id=? and event_id=?",
            Integer.class, TENANT_A, persisted.exportEventId())).isEqualTo(1);
        assertThat(history(record.id(), "EXPORT_REQUESTED")).isEqualTo(1);
    }

    private TransportBillingRecord validated(String prefix) {
        var created = service.create(preparer(TENANT_A), create(UUID.randomUUID()), key(prefix + "-create"));
        var replaced = service.replace(preparer(TENANT_A), created.id(), created.version(), replacement("100.00"));
        return service.validate(preparer(TENANT_A), replaced.id(), replaced.version());
    }

    private TransportBillingRecord approved(String prefix) {
        var record = validated(prefix);
        return service.approve(approver(TENANT_A), record.id(), record.version(), key(prefix + "-approve"));
    }

    private TransportBillingRecord finalized(String prefix) {
        var record = approved(prefix);
        return service.finalizeRecord(preparer(TENANT_A), record.id(), record.version(), key(prefix + "-finalize"));
    }

    private static TransportBillingUseCase.Create create(UUID source) {
        return new TransportBillingUseCase.Create(SourceType.TRIP, source, "LKR", null);
    }

    private static TransportBillingUseCase.Replacement replacement(String amount) {
        return new TransportBillingUseCase.Replacement(List.of(new TransportBillingUseCase.Line(UUID.randomUUID(),
            LineCategory.BASE_CHARGE, "RATE", "SOURCE", BigDecimal.ONE, new BigDecimal(amount), new BigDecimal(amount))),
            null, List.of(new TransportBillingUseCase.CostCentre(UUID.randomUUID(), "OPS", new BigDecimal("100"),
                "Operations", "OWNER")));
    }

    private static TransportBillingUseCase.Context preparer(UUID tenant) {
        return new TransportBillingUseCase.Context(tenant, PREPARER, "race-preparer");
    }

    private static TransportBillingUseCase.Context approver(UUID tenant) {
        return new TransportBillingUseCase.Context(tenant, APPROVER, "race-approver");
    }

    private int history(UUID record, String action) {
        return jdbc.queryForObject("select count(*) from transport_billing_history where tenant_id=? "
            + "and billing_record_id=? and action=?", Integer.class, TENANT_A, record, action);
    }

    private int count(String table, String column, Object value) {
        if (!List.of("transport_billing_record", "transport_billing_source_claim").contains(table)
                || !List.of("source_id", "create_idempotency_key").contains(column)) {
            throw new IllegalArgumentException("unsafe test query");
        }
        return jdbc.queryForObject("select count(*) from " + table + " where tenant_id=? and " + column + "=?",
            Integer.class, TENANT_A, value);
    }

    @SafeVarargs
    private static <T> void assertOneWinner(List<Result<T>> results, Class<? extends Throwable>... failures) {
        assertThat(results.stream().filter(Result::succeeded)).hasSize(1);
        assertThat(results.stream().map(Result::failure).filter(failure -> failure != null
            && List.of(failures).stream().anyMatch(type -> type.isInstance(failure)))).hasSize(1);
    }

    private static String key(String prefix) { return prefix + "-" + UUID.randomUUID(); }

    private static <T> List<Result<T>> race(Callable<T> first, Callable<T> second) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<Result<T>>>();
            for (var work : List.of(first, second)) futures.add(executor.submit(() -> {
                barrier.await();
                try { return new Result<>(work.call(), null); }
                catch (Throwable failure) { return new Result<>(null, failure); }
            }));
            return List.of(futures.get(0).get(), futures.get(1).get());
        }
    }

    private record Result<T>(T value, Throwable failure) { boolean succeeded() { return failure == null; } }
}
