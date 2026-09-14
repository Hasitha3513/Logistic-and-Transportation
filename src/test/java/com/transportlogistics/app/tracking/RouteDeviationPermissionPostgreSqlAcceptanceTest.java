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

class RouteDeviationPermissionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final String CODES = "'ROUTE_DEVIATION_VIEW','ROUTE_DEVIATION_MANAGE',"
            + "'ROUTE_DEVIATION_EVENT_VIEW','ROUTE_DEVIATION_APPROVE'";
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void cleanV1ToV89SeedsExactlyFourPermissionsForAdministrativeRolesOnly() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("89");
        assertThat(permissionCount()).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM app_role_permission grant_row
                JOIN app_role role ON role.id=grant_row.role_id
                WHERE grant_row.permission_code IN (%s)
                  AND role.name NOT IN ('ADMIN','LOCAL_MVP_ADMIN')
                """.formatted(CODES), Integer.class)).isZero();
    }

    @Test
    void v88ToV89GrantsOnlyExistingAdministrativeRoles() {
        flyway.clean();
        Flyway to88 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("88")).load();
        to88.migrate();
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,'ADMIN','Admin',TRUE)",
                UUID.randomUUID());
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,"
                + "'LOCAL_MVP_ADMIN','Local admin',TRUE)", UUID.randomUUID());
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,"
                + "'ROUTE_OPERATOR','Operator',TRUE)", UUID.randomUUID());
        flyway.migrate();
        assertThat(permissionCount()).isEqualTo(4);
        assertThat(grants("ADMIN")).isEqualTo(4);
        assertThat(grants("LOCAL_MVP_ADMIN")).isEqualTo(4);
        assertThat(grants("ROUTE_OPERATOR")).isZero();
    }

    private int permissionCount() {
        return jdbc.queryForObject("SELECT count(*) FROM app_permission WHERE code IN ("
                + CODES + ") AND active", Integer.class);
    }

    private int grants(String role) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM app_role_permission grant_row
                JOIN app_role role ON role.id=grant_row.role_id
                WHERE role.name=? AND grant_row.permission_code IN (%s)
                """.formatted(CODES), Integer.class, role);
    }
}
