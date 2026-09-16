package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TrackingDashboardPermissionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void v94SeedsOnlyDashboardViewAndGrantsExistingOperatorRoles() {
        assertThat(jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("98");
        assertThat(jdbc.queryForList(
                "SELECT code FROM app_permission WHERE code='TRACKING_DASHBOARD_VIEW'", String.class))
                .containsExactly("TRACKING_DASHBOARD_VIEW");
        assertThat(jdbc.queryForList("""
                SELECT r.name FROM app_role_permission rp
                JOIN app_role r ON r.id=rp.role_id
                WHERE rp.permission_code='TRACKING_DASHBOARD_VIEW'
                ORDER BY r.name
                """, String.class)).allMatch(name ->
                        name.equals("ADMIN") || name.equals("LOCAL_MVP_ADMIN") || name.equals("DISPATCHER"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version='94' AND success", Integer.class))
                .isEqualTo(1);
    }
}
