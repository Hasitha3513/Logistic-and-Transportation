package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CustomerSelfServicePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test void flywayCurrentHeadIsV64AndTenantConsistentForeignKeyIsEnforced() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("64");
        UUID tenant = UUID.randomUUID(); UUID delivery = insertOrder(tenant); UUID access = UUID.randomUUID();
        insertAccess(access, tenant, delivery, "a".repeat(64), "attempt-acceptance-000001");
        assertThatThrownBy(() -> insertAccess(UUID.randomUUID(), UUID.randomUUID(), delivery,
                "b".repeat(64), "attempt-acceptance-000002"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void tokenHashIdempotencyAndFeedbackUniquenessAreDatabaseEnforced() {
        UUID tenant = UUID.randomUUID(); UUID customer = UUID.randomUUID(); UUID delivery = insertOrder(tenant, customer);
        UUID firstAccess = UUID.randomUUID(); UUID secondAccess = UUID.randomUUID();
        insertAccess(firstAccess, tenant, delivery, "c".repeat(64), "attempt-acceptance-000003");
        assertThatThrownBy(() -> insertAccess(secondAccess, tenant, delivery, "c".repeat(64), "attempt-acceptance-000004"))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertAccess(secondAccess, tenant, delivery, "d".repeat(64), "attempt-acceptance-000004");
        insertFeedback(UUID.randomUUID(), tenant, delivery, customer, firstAccess, "feedback-key-00000001");
        assertThatThrownBy(() -> insertFeedback(UUID.randomUUID(), tenant, delivery, customer, secondAccess,
                "feedback-key-00000002")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAccess(UUID.randomUUID(), tenant, delivery, "1".repeat(64),
                "attempt-acceptance-000004")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void submissionIdempotencyAndAccessTenantBindingAreDatabaseEnforced() {
        UUID tenantA = UUID.randomUUID(); UUID customerA = UUID.randomUUID();
        UUID deliveryA = insertOrder(tenantA, customerA); UUID accessA = UUID.randomUUID();
        insertAccess(accessA, tenantA, deliveryA, "2".repeat(64), "attempt-acceptance-000005");
        insertIssue(UUID.randomUUID(), tenantA, deliveryA, customerA, accessA,
                "issue-key-acceptance-01", "3".repeat(64));
        assertThatThrownBy(() -> insertIssue(UUID.randomUUID(), tenantA, deliveryA, customerA, accessA,
                "issue-key-acceptance-01", "4".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);

        UUID tenantB = UUID.randomUUID(); UUID customerB = UUID.randomUUID();
        UUID deliveryB = insertOrder(tenantB, customerB); UUID accessB = UUID.randomUUID();
        insertAccess(accessB, tenantB, deliveryB, "5".repeat(64), "attempt-acceptance-000006");
        assertThatThrownBy(() -> insertIssue(UUID.randomUUID(), tenantA, deliveryA, customerA, accessB,
                "issue-key-acceptance-02", "6".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void requiredTokenAndSubmissionIndexesExistWithTenantLeadingColumns() {
        var definitions = jdbc.queryForList("select indexdef from pg_indexes where schemaname='public' "
                + "and tablename in ('delivery_self_service_access','delivery_customer_submission')",
                String.class);
        assertThat(definitions).anyMatch(value -> value.contains("token_hash"));
        assertThat(definitions).anyMatch(value -> value.contains("tenant_id, delivery_order_id, customer_id"));
        assertThat(definitions).anyMatch(value -> value.contains("submission_type") && value.contains("created_at DESC"));
    }

    private UUID insertOrder(UUID tenant) { return insertOrder(tenant, UUID.randomUUID()); }
    private UUID insertOrder(UUID tenant, UUID customer) {
        UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.parse("2026-09-03T00:00:00Z");
        jdbc.update("""
            insert into delivery_order(id,tenant_id,delivery_number,customer_id,origin_location_id,destination_location_id,
             priority,service_type,window_start,window_end,status,version,created_at,updated_at,created_by,updated_by)
            values(?,?,?,?,?,?,'NORMAL','STANDARD',?,?,'DRAFT',0,?,?,'test','test')
            """, id, tenant, "DEL-" + id.toString().substring(0, 11), customer, UUID.randomUUID(), UUID.randomUUID(),
                now.plusHours(1), now.plusHours(2), now, now);
        return id;
    }
    private void insertAccess(UUID id, UUID tenant, UUID delivery, String hash, String key) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T00:00:00Z");
        UUID customer = jdbc.queryForObject("select customer_id from delivery_order where id=?", UUID.class, delivery);
        jdbc.update("""
            insert into delivery_self_service_access(id,tenant_id,delivery_order_id,customer_id,recipient_contact_hash,
             contact_hash_key_version,token_hash,allowed_actions,issuance_idempotency_key,issued_at,expires_at,use_count,
             version,created_at,updated_at,created_by,updated_by)
            values(?,?,?,?,?,'v1',?,ARRAY['TRACK']::varchar[],?,?,?,0,0,?,?,'test','test')
            """, id, tenant, delivery, customer, "e".repeat(64), hash, key, now, now.plusDays(30), now, now);
    }
    private void insertFeedback(UUID id, UUID tenant, UUID delivery, UUID customer, UUID access, String key) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T00:00:00Z");
        jdbc.update("""
            insert into delivery_customer_submission(id,tenant_id,delivery_order_id,customer_id,access_id,submission_type,
             rating,status,idempotency_key,request_hash,created_at,updated_at,version,created_by,updated_by)
            values(?,?,?,?,?,'FEEDBACK',5,'RECORDED',?,?,?, ?,0,'test','test')
            """, id, tenant, delivery, customer, access, key, "f".repeat(64), now, now);
    }
    private void insertIssue(UUID id, UUID tenant, UUID delivery, UUID customer, UUID access,
                             String key, String hash) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T00:00:00Z");
        jdbc.update("""
            insert into delivery_customer_submission(id,tenant_id,delivery_order_id,customer_id,access_id,submission_type,
             category,description,status,idempotency_key,request_hash,created_at,updated_at,version,created_by,updated_by)
            values(?,?,?,?,?,'ISSUE','OTHER','A detailed customer issue.','SUBMITTED',?,?,?, ?,0,'test','test')
            """, id, tenant, delivery, customer, access, key, hash, now, now);
    }
}
