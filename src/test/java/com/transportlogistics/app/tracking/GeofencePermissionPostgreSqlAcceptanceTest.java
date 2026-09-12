package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GeofencePermissionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final String CODES =
            "'GEOFENCE_VIEW','GEOFENCE_MANAGE','GEOFENCE_EVENT_VIEW'";

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void cleanV1ToCurrentHeadRetainsExactlyThreePermissionsAndOnlyAdministrativeGrants() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("82");
        assertThat(permissionCount()).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM app_role_permission grant_row
                JOIN app_role role ON role.id=grant_row.role_id
                WHERE grant_row.permission_code IN (%s)
                  AND role.name NOT IN ('ADMIN','LOCAL_MVP_ADMIN')
                """.formatted(CODES), Integer.class)).isZero();
    }

    @Test
    void v77ToV78GrantsAnExistingLocalAdministratorWithoutBroadeningOtherRoles() {
        flyway.clean();
        Flyway to77 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("77")).load();
        to77.migrate();
        UUID adminRole = UUID.randomUUID();
        UUID localRole = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_role(id,name,description,active)
                VALUES(?,'ADMIN','Administrator',TRUE)
                """, adminRole);
        jdbc.update("""
                INSERT INTO app_role(id,name,description,active)
                VALUES(?,'LOCAL_MVP_ADMIN','Local administrator',TRUE)
                """, localRole);

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("82");
        assertThat(permissionCount()).isEqualTo(3);
        assertThat(grantCount("ADMIN")).isEqualTo(3);
        assertThat(grantCount("LOCAL_MVP_ADMIN")).isEqualTo(3);
    }

    private int permissionCount() {
        return jdbc.queryForObject("SELECT count(*) FROM app_permission WHERE code IN ("
                + CODES + ") AND active", Integer.class);
    }

    private int grantCount(String roleName) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM app_role_permission grant_row
                JOIN app_role role ON role.id=grant_row.role_id
                WHERE role.name=? AND grant_row.permission_code IN (%s)
                """.formatted(CODES), Integer.class, roleName);
    }
}
