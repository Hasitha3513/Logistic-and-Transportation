package com.transportlogistics.app.system.infrastructure.config;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgreSqlSampleDataRbacIdempotencyTest extends PostgreSqlIntegrationTest {
    private static final UUID EXISTING_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXISTING_ADMIN_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final String EXISTING_ADMIN_PASSWORD_HASH = "bootstrap-owned-password-hash";

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @BeforeEach
    void restoreProductionBaseline() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void reusesAnExistingAdminRoleIdForPermissionsAndMembershipLinksOnRepeatedSeeds() {
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, 'ADMIN', 'Existing admin', true)",
                EXISTING_ADMIN_ID);
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, 'admin', 'admin@localhost.test', ?, 'Local', 'Administrator', true,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, EXISTING_ADMIN_USER_ID, EXISTING_ADMIN_PASSWORD_HASH);

        runFixture();
        runFixture();

        assertEquals(EXISTING_ADMIN_ID, jdbc.queryForObject("SELECT id FROM app_role WHERE name = 'ADMIN'", UUID.class));
        assertEquals(EXISTING_ADMIN_PASSWORD_HASH,
                jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'admin'", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM app_role WHERE name = 'ADMIN'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM app_role WHERE id = '20000000-0000-0000-0000-000000000001'", Integer.class));
        assertEquals(jdbc.queryForObject("SELECT COUNT(*) FROM app_permission", Integer.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM app_role_permission WHERE role_id = ?", Integer.class,
                        EXISTING_ADMIN_ID));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM tenant_membership_role assignment
                JOIN tenant_membership membership ON membership.membership_id = assignment.membership_id
                JOIN app_user user_row ON user_row.id = membership.user_id
                WHERE user_row.username = 'user.kasun' AND assignment.role_id = ?
                """, Integer.class, EXISTING_ADMIN_ID));
        assertEquals(10, jdbc.queryForObject("""
                SELECT COUNT(*) FROM delivery_order
                WHERE id BETWEEN 'a0000000-0000-0000-0000-000000000001'::uuid
                             AND 'a0000000-0000-0000-0000-000000000010'::uuid
                  AND delivery_number ~ '^DEL-[0-9]{4}-[0-9]{6}$'
                """, Integer.class));
        assertEquals(5, jdbc.queryForObject("""
                SELECT COUNT(*) FROM fuel_card
                WHERE tenant_id = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a'
                  AND id BETWEEN 'c0000000-0000-0000-0000-000000000001'::uuid
                             AND 'c0000000-0000-0000-0000-000000000005'::uuid
                """, Integer.class));
        assertEquals(5, jdbc.queryForObject("""
                SELECT COUNT(*) FROM fuel_card_transaction
                WHERE tenant_id = '4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a'
                  AND id BETWEEN 'c4000000-0000-0000-0000-000000000001'::uuid
                             AND 'c4000000-0000-0000-0000-000000000005'::uuid
                """, Integer.class));
        assertCanonicalBunkerLedgerTails();
    }

    private void assertCanonicalBunkerLedgerTails() {
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM bunker_tank tank
                WHERE tank.current_stock_liters <> 0
                  AND NOT EXISTS (
                      SELECT 1 FROM bunker_stock_movement movement
                      WHERE movement.tenant_id = tank.tenant_id AND movement.tank_id = tank.id)
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM bunker_tank tank
                WHERE EXISTS (
                    SELECT 1 FROM bunker_stock_movement movement
                    WHERE movement.tenant_id = tank.tenant_id AND movement.tank_id = tank.id)
                  AND tank.current_stock_liters <> (
                      SELECT movement.resulting_balance_liters
                      FROM bunker_stock_movement movement
                      WHERE movement.tenant_id = tank.tenant_id AND movement.tank_id = tank.id
                      ORDER BY movement.ledger_sequence DESC
                      LIMIT 1)
                """, Integer.class));
    }

    private void runFixture() {
        var populator = new ResourceDatabasePopulator(new ClassPathResource("db/sample-data/postgresql-sample-data.sql"));
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
