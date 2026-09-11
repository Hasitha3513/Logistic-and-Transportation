package com.transportlogistics.app.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.notification.application.ports.out.NotificationRulePolicyRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationEventCatalogue;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import com.transportlogistics.app.notification.domain.model.NotificationTemplateRenderer;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GeofenceNotificationCatalogPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final String EVENT_TYPE = "VEHICLE_GEOFENCE_TRANSITIONED_V1";

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;
    @Autowired NotificationRuleRepository ruleRepository;
    @Autowired NotificationRulePolicyRepository policyRepository;
    @Autowired NotificationTemplateRepository templateRepository;

    @Test
    void cleanV1ToV79SeedsOneSafeUsableTemplateAndOneRulePerTenant() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("80");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_template
                WHERE event_type=? AND code=? AND channel='IN_APP' AND version=1 AND active
                """, Integer.class, EVENT_TYPE, EVENT_TYPE)).isOne();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule
                WHERE event_type=? AND channel='IN_APP' AND recipient_type='ROLE'
                  AND recipient_value='DISPATCHER' AND template_code=? AND enabled
                """, Integer.class, EVENT_TYPE, EVENT_TYPE))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM tenant", Integer.class));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_rule_policy policy
                JOIN notification_rule rule ON rule.id=policy.rule_id AND rule.tenant_id=policy.tenant_id
                WHERE rule.event_type=? AND policy.suppression_window_minutes=0
                  AND NOT policy.quiet_hours_enabled AND NOT policy.escalation_enabled
                """, Integer.class, EVENT_TYPE))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM tenant", Integer.class));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notification_template
                WHERE event_type=? AND channel<>'IN_APP'
                """, Integer.class, EVENT_TYPE)).isZero();

        var rules = ruleRepository.findByEventTypeAndEnabledTrue(EVENT_TYPE);
        assertThat(rules).hasSize(1);
        var rule = rules.getFirst();
        assertThat(policyRepository.findByRuleId(rule.id())).isPresent();
        assertThat(templateRepository.findActiveCompatible(
                rule.templateCode(), rule.eventType(), rule.channel())).isPresent();
    }

    @Test
    void seededTemplateRendersOnlyFrozenSafeFactsAndUnknownEventDoesNotMatch() {
        NotificationTemplate template = jdbc.queryForObject("""
                SELECT id,code,name,event_type,channel,subject,body,version,active,created_at,updated_at
                FROM notification_template WHERE event_type=?
                """, (resultSet, rowNumber) -> new NotificationTemplate(
                    resultSet.getObject("id", UUID.class), resultSet.getString("code"),
                    resultSet.getString("name"), resultSet.getString("event_type"),
                    NotificationChannel.valueOf(resultSet.getString("channel")),
                    resultSet.getString("subject"), resultSet.getString("body"),
                    resultSet.getInt("version"), resultSet.getBoolean("active"),
                    resultSet.getObject("created_at", OffsetDateTime.class),
                    resultSet.getObject("updated_at", OffsetDateTime.class)), EVENT_TYPE);
        var rendered = new NotificationTemplateRenderer().render(template, Map.of(
                "eventTime", "2026-09-10T12:00:00Z", "severity", "HIGH",
                "geofenceId", UUID.randomUUID().toString(), "vehicleId", "VEHICLE-42",
                "geofenceType", "UNAUTHORIZED_ZONE", "transition", "UNAUTHORIZED_ZONE_ENTERED",
                "sourceTimestamp", "2026-09-10T11:59:58Z", "definitionVersion", "3"));

        assertThat(rendered.subject()).contains("VEHICLE-42");
        assertThat(rendered.body()).contains("UNAUTHORIZED_ZONE_ENTERED", "UNAUTHORIZED_ZONE")
                .doesNotContainIgnoringCase("latitude", "longitude", "polygon", "device", "provider",
                        "imei", "driver", "customer", "credential", "rawTelemetry");
        assertThat(NotificationEventCatalogue.find("UNRELATED_TRACKING_EVENT")).isEmpty();
    }

    @Test
    void v78ToV79CreatesOnlyTheAuthorizedCatalogRows() {
        flyway.clean();
        Flyway to78 = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(flyway.getConfiguration().getPlaceholders())
                .target(MigrationVersion.fromVersion("78")).load();
        to78.migrate();
        int templatesBefore = jdbc.queryForObject("SELECT count(*) FROM notification_template", Integer.class);
        int rulesBefore = jdbc.queryForObject("SELECT count(*) FROM notification_rule", Integer.class);
        int policiesBefore = jdbc.queryForObject("SELECT count(*) FROM notification_rule_policy", Integer.class);
        int tenants = jdbc.queryForObject("SELECT count(*) FROM tenant", Integer.class);

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("80");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_template", Integer.class))
                .isEqualTo(templatesBefore + 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_rule", Integer.class))
                .isEqualTo(rulesBefore + tenants);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_rule_policy", Integer.class))
                .isEqualTo(policiesBefore + tenants);
    }
}
