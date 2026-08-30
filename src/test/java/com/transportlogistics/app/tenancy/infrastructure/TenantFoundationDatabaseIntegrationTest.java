package com.transportlogistics.app.tenancy.infrastructure;

import com.transportlogistics.app.tenancy.CanonicalTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TenantFoundationDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    private UUID testUser;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM tenant WHERE tenant_id != ?", CanonicalTenant.ID);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (testUser != null) {
            jdbc.update("DELETE FROM tenant_membership WHERE user_id = ?", testUser);
            jdbc.update("DELETE FROM app_user WHERE id = ?", testUser);
        }
    }

    @Test
    void cleanMigrationCreatesOnlyTheCanonicalTenant() {
        var tenant = jdbc.queryForMap("SELECT * FROM tenant WHERE tenant_id = ?", CanonicalTenant.ID);

        assertThat(tenant.get("tenant_code")).isEqualTo("CLTS-LK");
        assertThat(tenant.get("tenant_name")).isEqualTo("Ceylon Logistics & Transport Solutions (Pvt) Ltd");
        assertThat(tenant.get("default_currency")).isEqualTo("LKR");
        assertThat(tenant.get("default_time_zone")).isEqualTo("Asia/Colombo");
        assertThat(tenant.get("status")).isEqualTo("ACTIVE");
        assertThat(tenant.get("version")).isEqualTo(0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tenant", Long.class)).isOne();
    }

    @Test
    void databaseEnforcesMembershipCardinalityAndForeignKeys() {
        testUser = UUID.randomUUID();
        insertUser(testUser, "tenant-user-a");
        insertMembership(UUID.randomUUID(), CanonicalTenant.ID, testUser);

        assertThatThrownBy(() -> insertMembership(UUID.randomUUID(), CanonicalTenant.ID, testUser))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertMembership(UUID.randomUUID(), CanonicalTenant.ID, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertMembership(UUID.randomUUID(), UUID.randomUUID(), testUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseEnforcesUniqueTenantCode() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tenant (tenant_id, tenant_code, tenant_name, default_currency, default_time_zone,
                    status, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, 'CLTS-LK', 'Duplicate', 'LKR', 'Asia/Colombo', 'ACTIVE', ?, 'test', ?, 'test', 0)
                """, UUID.randomUUID(), Timestamp.from(Instant.now()), Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertUser(UUID userId, String username) {
        var now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO app_user (id, username, email, password_hash, first_name, last_name, active,
                    created_at, updated_at) VALUES (?, ?, ?, 'hash', 'Tenant', 'User', TRUE, ?, ?)
                """, userId, username, username + "@example.test", now, now);
    }

    private void insertMembership(UUID membershipId, UUID tenantId, UUID userId) {
        var now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO tenant_membership (membership_id, tenant_id, user_id, status, created_at, created_by,
                    updated_at, updated_by, version) VALUES (?, ?, ?, 'ACTIVE', ?, 'test', ?, 'test', 0)
                """, membershipId, tenantId, userId, now, now);
    }
}
