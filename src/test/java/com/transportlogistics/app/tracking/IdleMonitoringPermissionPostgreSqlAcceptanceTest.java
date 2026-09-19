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

class IdleMonitoringPermissionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;
    @Test void v104SeedsOnlyApprovedPermissionsAndRoleGrants(){
        flyway.clean();
        Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("103")).load().migrate();
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,'ADMIN','Administrator',TRUE)",UUID.randomUUID());
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,'DISPATCHER','Dispatcher',TRUE)",UUID.randomUUID());
        jdbc.update("INSERT INTO app_role(id,name,description,active) VALUES(?,'LOCAL_MVP_ADMIN','Local admin',TRUE)",UUID.randomUUID());
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT max(version::integer) FROM flyway_schema_history WHERE success",Integer.class)).isEqualTo(104);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_permission WHERE code IN ('IDLE_MONITOR_VIEW','IDLE_EVENT_VIEW') AND active",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_role_permission rp JOIN app_role r ON r.id=rp.role_id WHERE rp.permission_code IN ('IDLE_MONITOR_VIEW','IDLE_EVENT_VIEW') AND r.name IN ('ADMIN','DISPATCHER')",Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_role_permission rp JOIN app_role r ON r.id=rp.role_id WHERE rp.permission_code IN ('IDLE_MONITOR_VIEW','IDLE_EVENT_VIEW') AND r.name='LOCAL_MVP_ADMIN'",Integer.class)).isZero();
    }
}
