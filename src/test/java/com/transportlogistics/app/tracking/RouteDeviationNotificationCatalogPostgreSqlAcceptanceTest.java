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

class RouteDeviationNotificationCatalogPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test void v89ToV90SeedsOnlyTwoDispatcherInAppRulesAndTemplates() {
        flyway.clean();
        Flyway to89 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("89")).load();
        to89.migrate();
        UUID tenant = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenant(tenant_id,tenant_code,tenant_name,default_currency,default_time_zone,status,
                 created_at,created_by,updated_at,updated_by,version)
                VALUES(?, 'RD-NOTIFY', 'Route deviation notifications', 'LKR', 'UTC', 'ACTIVE',
                 now(), 'test', now(), 'test', 0)
                """, tenant);

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("95");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_template
                WHERE code IN ('TRACKING_ROUTE_DEVIATION_DETECTED_V1',
                               'TRACKING_ROUTE_DEVIATION_ESCALATED_V1')
                  AND channel='IN_APP' AND active
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule
                WHERE tenant_id=? AND event_type LIKE 'VEHICLE_ROUTE_DEVIATION_%_V1'
                  AND channel='IN_APP' AND recipient_type='ROLE' AND recipient_value='DISPATCHER'
                  AND enabled
                """, Integer.class, tenant)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule rule
                JOIN notification_rule_policy policy ON policy.rule_id=rule.id
                WHERE rule.tenant_id=? AND rule.event_type LIKE 'VEHICLE_ROUTE_DEVIATION_%_V1'
                """, Integer.class, tenant)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule
                WHERE event_type LIKE 'VEHICLE_ROUTE_DEVIATION_%_V1' AND channel<>'IN_APP'
                """, Integer.class)).isZero();
    }
}
