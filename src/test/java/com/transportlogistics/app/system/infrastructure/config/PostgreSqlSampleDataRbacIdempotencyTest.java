package com.transportlogistics.app.system.infrastructure.config;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
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

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test
    void reusesAnExistingAdminRoleIdForPermissionsAndMembershipLinksOnRepeatedSeeds() {
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, 'ADMIN', 'Existing admin', true)",
                EXISTING_ADMIN_ID);

        runFixture();
        runFixture();

        assertEquals(EXISTING_ADMIN_ID, jdbc.queryForObject("SELECT id FROM app_role WHERE name = 'ADMIN'", UUID.class));
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
    }

    private void runFixture() {
        var populator = new ResourceDatabasePopulator(new ClassPathResource("db/sample-data/postgresql-sample-data.sql"));
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
