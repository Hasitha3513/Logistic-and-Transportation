package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class JourneyReplayPermissionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void v93SeedsOnlyTheTwoReplayPermissionsAndGrantsExistingOperatorRoles() {
        assertThat(jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("104");
        assertThat(jdbc.queryForList(
                "SELECT code FROM app_permission WHERE code LIKE 'JOURNEY_REPLAY_%' ORDER BY code",
                String.class)).containsExactly("JOURNEY_REPLAY_INCIDENT_VIEW", "JOURNEY_REPLAY_VIEW");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM app_role_permission rp
                JOIN app_role r ON r.id=rp.role_id
                WHERE rp.permission_code LIKE 'JOURNEY_REPLAY_%'
                  AND r.name NOT IN ('ADMIN','LOCAL_MVP_ADMIN','DISPATCHER')
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='93' AND success",
                Integer.class)).isEqualTo(1);
    }
}
