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

    @Test void flywayCurrentHeadIsV59AndTenantConsistentForeignKeyIsEnforced() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("59");
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
             version,created_at,updated_at) values(?,?,?,?,?,'v1',?,ARRAY['TRACK']::varchar[],?,?,?,0,0,?,?)
            """, id, tenant, delivery, customer, "e".repeat(64), hash, key, now, now.plusDays(30), now, now);
    }
    private void insertFeedback(UUID id, UUID tenant, UUID delivery, UUID customer, UUID access, String key) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T00:00:00Z");
        jdbc.update("""
            insert into delivery_customer_submission(id,tenant_id,delivery_order_id,customer_id,access_id,submission_type,
             rating,status,idempotency_key,request_hash,created_at,updated_at,version)
            values(?,?,?,?,?,'FEEDBACK',5,'SUBMITTED',?,?,?, ?,0)
            """, id, tenant, delivery, customer, access, key, "f".repeat(64), now, now);
    }
}
