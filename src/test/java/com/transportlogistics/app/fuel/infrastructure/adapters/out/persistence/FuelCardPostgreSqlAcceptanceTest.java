package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.OffsetDateTime;
import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelCardPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired private JdbcClient jdbc;

    @Test
    void currentHeadContainsUs35TablesAndPermissions() {
        assertThat(jdbc.sql("select version from flyway_schema_history where success order by installed_rank desc limit 1")
                .query(String.class).single()).isEqualTo("64");
        assertThat(jdbc.sql("select count(*) from information_schema.tables where table_schema='public' and table_name like 'fuel_card%'")
                .query(Integer.class).single()).isEqualTo(8);
        assertThat(jdbc.sql("select count(*) from app_permission where code like 'FUEL_CARD_%' and active")
                .query(Integer.class).single()).isEqualTo(5);
    }

    @Test
    void tenantConsistencyAndOneActiveBindingAreDatabaseEnforced() {
        var tenantA = UUID.randomUUID(); var tenantB = UUID.randomUUID(); var card = insertCard(tenantA, "ref-a");
        insertBinding(tenantA, card, UUID.randomUUID());
        assertThatThrownBy(() -> insertBinding(tenantA, card, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertBinding(tenantB, card, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void providerReferenceBatchFileAndTransactionIdentityAreTenantScopedAndUnique() {
        var tenant = UUID.randomUUID(); var provider = UUID.randomUUID(); var card = insertCard(tenant, provider, "ref-a");
        assertThatThrownBy(() -> insertCard(tenant, provider, "ref-a")).isInstanceOf(DataIntegrityViolationException.class);
        var batch = insertBatch(tenant, provider, "batch-a", "a".repeat(64));
        assertThatThrownBy(() -> insertBatch(tenant, provider, "batch-a", "b".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertBatch(tenant, provider, "batch-b", "a".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        insertTransaction(tenant, batch, provider, card, "transaction-a");
        assertThatThrownBy(() -> insertTransaction(tenant, batch, provider, card, "transaction-a"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void providerFactsAndAppendOnlyHistoriesHaveNoUpdateOrDeleteApplicationContract() {
        assertThat(jdbc.sql("select count(*) from information_schema.columns where table_name='fuel_card_transaction' and column_name in ('provider_transaction_id','canonical_hash','provider_status')")
                .query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("select count(*) from information_schema.tables where table_name in ('fuel_card_binding_history','fuel_card_reconciliation_history','fuel_card_audit_event')")
                .query(Integer.class).single()).isEqualTo(3);
    }

    @Test
    void concurrentLifecycleCommandsUseOneOptimisticVersionWinner() throws Exception {
        var tenant = UUID.randomUUID(); var card = insertCard(tenant, "race-card");
        assertThat(raceUpdate("update fuel_card set status='BLOCKED',version=version+1 where id=? and tenant_id=? and version=0",
                card, tenant)).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void concurrentBindingAndRestrictionCommandsHaveOneWinner() throws Exception {
        var tenant = UUID.randomUUID(); var card = insertCard(tenant, "race-binding");
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        Callable<Integer> insert = () -> {
            start.await();
            try (var connection = DriverManager.getConnection(configuredJdbcUrl(), configuredDatabaseUsername(), configuredDatabasePassword());
                 var statement = connection.prepareStatement("insert into fuel_card_binding_history (id,tenant_id,card_id,binding_type,binding_id,effective_from,reason,changed_by,created_at) values (?,?,?,?,?,?,?,?,?)")) {
                statement.setObject(1, UUID.randomUUID()); statement.setObject(2, tenant); statement.setObject(3, card);
                statement.setString(4, "VEHICLE"); statement.setObject(5, UUID.randomUUID()); statement.setObject(6, OffsetDateTime.now());
                statement.setString(7, "race"); statement.setObject(8, UUID.randomUUID()); statement.setObject(9, OffsetDateTime.now());
                try { return statement.executeUpdate(); } catch (java.sql.SQLException conflict) { return 0; }
            }
        };
        var first = pool.submit(insert); var second = pool.submit(insert); start.countDown();
        assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(0, 1); pool.shutdownNow();
    }

    @Test
    void concurrentReconciliationAndReversalDispositionUseOneVersionWinner() throws Exception {
        var tenant = UUID.randomUUID(); var provider = UUID.randomUUID(); var card = insertCard(tenant, provider, "race-transaction");
        var batch = insertBatch(tenant, provider, "race-batch", "e".repeat(64)); insertTransaction(tenant, batch, provider, card, "race-transaction");
        var transactionId = jdbc.sql("select id from fuel_card_transaction where tenant_id=:tenant and provider_transaction_id='race-transaction'")
                .param("tenant", tenant).query(UUID.class).single();
        assertThat(raceUpdate("update fuel_card_transaction set local_status='REVIEW_REQUIRED',version=version+1 where id=? and tenant_id=? and version=0",
                transactionId, tenant)).containsExactlyInAnyOrder(0, 1);
    }

    private java.util.List<Integer> raceUpdate(String sql, UUID id, UUID tenant) throws Exception {
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        Callable<Integer> update = () -> { start.await(); try (var connection = DriverManager.getConnection(configuredJdbcUrl(), configuredDatabaseUsername(), configuredDatabasePassword());
                var statement = connection.prepareStatement(sql)) { statement.setObject(1, id); statement.setObject(2, tenant); return statement.executeUpdate(); } };
        var first = pool.submit(update); var second = pool.submit(update); start.countDown();
        var result = java.util.List.of(first.get(), second.get()); pool.shutdownNow(); return result;
    }

    private UUID insertCard(UUID tenant, String reference) { return insertCard(tenant, UUID.randomUUID(), reference); }
    private UUID insertCard(UUID tenant, UUID provider, String reference) {
        var id = UUID.randomUUID(); var now = OffsetDateTime.now();
        jdbc.sql("""
                insert into fuel_card (id,tenant_id,provider_id,alias,provider_card_reference,provider_reference_hash,
                masked_identifier,last_four,expiry_month,expiry_year,status,version,created_by,created_at,updated_at)
                values (:id,:tenant,:provider,'Acceptance card',:reference,:hash,'**** 4242','4242',12,2028,'DRAFT',0,:actor,:now,:now)
                """).param("id", id).param("tenant", tenant).param("provider", provider).param("reference", reference)
                .param("hash", "c".repeat(64)).param("actor", UUID.randomUUID()).param("now", now).update();
        return id;
    }

    private void insertBinding(UUID tenant, UUID card, UUID target) {
        jdbc.sql("""
                insert into fuel_card_binding_history (id,tenant_id,card_id,binding_type,binding_id,effective_from,reason,changed_by,created_at)
                values (:id,:tenant,:card,'VEHICLE',:target,:now,'acceptance',:actor,:now)
                """).param("id", UUID.randomUUID()).param("tenant", tenant).param("card", card).param("target", target)
                .param("now", OffsetDateTime.now()).param("actor", UUID.randomUUID()).update();
    }

    private UUID insertBatch(UUID tenant, UUID provider, String providerBatchId, String hash) {
        var id = UUID.randomUUID(); var now = OffsetDateTime.now();
        jdbc.sql("""
                insert into fuel_card_import_batch (id,tenant_id,provider_id,provider_batch_id,file_hash,generated_at,
                transaction_count,imported_count,review_count,imported_by,created_at)
                values (:id,:tenant,:provider,:batch,:hash,:now,1,1,0,:actor,:now)
                """).param("id", id).param("tenant", tenant).param("provider", provider).param("batch", providerBatchId)
                .param("hash", hash).param("actor", UUID.randomUUID()).param("now", now).update();
        return id;
    }

    private void insertTransaction(UUID tenant, UUID batch, UUID provider, UUID card, String providerTransactionId) {
        var now = OffsetDateTime.now();
        jdbc.sql("""
                insert into fuel_card_transaction (id,tenant_id,batch_id,provider_id,card_id,provider_transaction_id,
                canonical_hash,transaction_kind,transaction_timestamp,fuel_type,quantity_litres,unit_price,total_amount,
                currency,provider_status,local_status,imported_by,version,created_at)
                values (:id,:tenant,:batch,:provider,:card,:transaction,:hash,'PURCHASE',:now,'DIESEL',10,300,3000,
                'LKR','POSTED','IMPORTED',:actor,0,:now)
                """).param("id", UUID.randomUUID()).param("tenant", tenant).param("batch", batch).param("provider", provider)
                .param("card", card).param("transaction", providerTransactionId).param("hash", "d".repeat(64))
                .param("actor", UUID.randomUUID()).param("now", now).update();
    }
}
