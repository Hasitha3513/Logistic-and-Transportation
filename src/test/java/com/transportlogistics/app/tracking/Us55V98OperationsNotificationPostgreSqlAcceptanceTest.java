package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class Us55V98OperationsNotificationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final Flyway flyway;
    @Autowired Us55V98OperationsNotificationPostgreSqlAcceptanceTest(DataSource dataSource, Flyway flyway) {
        this.dataSource = dataSource;
        this.flyway = flyway;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Test void v98SeedsExactExistingTenantInAppContractAndTrackingConstraints() {
        flyway.clean();
        Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("97")).load().migrate();
        UUID tenant = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenant(tenant_id,tenant_code,tenant_name,default_currency,default_time_zone,status,
                 created_at,created_by,updated_at,updated_by,version)
                VALUES(?, 'GPS-NOTIFY', 'GPS notifications', 'LKR', 'UTC', 'ACTIVE',
                 now(), 'test', now(), 'test', 0)
                """, tenant);
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success "
                + "ORDER BY installed_rank DESC LIMIT 1", String.class)).isEqualTo("102");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_template WHERE "
                + "code='TRACKING_GPS_EXCEPTION_ALERT_V1' AND event_type='TRACKING_GPS_EXCEPTION_OPENED' "
                + "AND channel='IN_APP' AND active", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_rule WHERE tenant_id=? AND "
                + "event_type='TRACKING_GPS_EXCEPTION_OPENED' AND channel='IN_APP' AND "
                + "recipient_type='ROLE' AND recipient_value='DISPATCHER' AND severity_threshold='WARNING'",
                Integer.class, tenant)).isOne();
        assertThat(jdbc.queryForObject("SELECT check_clause FROM information_schema.check_constraints "
                + "WHERE constraint_name='ck_operational_exception_source_module'", String.class))
                .contains("TRACKING");
        assertThat(jdbc.queryForObject("SELECT check_clause FROM information_schema.check_constraints "
                + "WHERE constraint_name='ck_operational_exception_category'", String.class))
                .contains("TRACKING_CONNECTIVITY", "TRACKING_DEVICE_HEALTH",
                        "TRACKING_DEVICE_SECURITY", "TRACKING_DATA_QUALITY");
    }
}
