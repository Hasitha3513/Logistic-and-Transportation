package com.transportlogistics.app.billing.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class BillingV72PostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test void v72CreatesTenantOwnedBillingSchemaAndFivePermissions(){
        assertThat(jdbc.queryForObject("select version from flyway_schema_history where success order by installed_rank desc limit 1",String.class)).isEqualTo("83");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name like 'transport_billing_%'",Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from app_permission where code in ('BILLING_VIEW','BILLING_PREPARE','BILLING_APPROVE','BILLING_FINALIZE','BILLING_EXPORT') and active",Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname='public' and tablename like 'transport_billing_%' and indexdef like '%(tenant_id,%'",Integer.class)).isGreaterThanOrEqualTo(9);
    }
}
