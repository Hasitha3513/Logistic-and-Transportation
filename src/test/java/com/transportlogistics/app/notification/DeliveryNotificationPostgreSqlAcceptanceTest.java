package com.transportlogistics.app.notification;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryNotificationPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void v58AssetsRemainValidAtCurrentV65Head() {
        assertThat(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",
            Integer.class))
            .isEqualTo(65);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where version='58' and success",
            Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notification_template "
            + "where event_type like 'DELIVERY_%' and channel in ('EMAIL','SMS')", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from notification_rule "
            + "where recipient_type='EVENT_CUSTOMER'", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes "
            + "where indexname='idx_notif_execution_tenant_aggregate_created'", Integer.class)).isEqualTo(1);
    }

    @Test
    void preferenceBusinessKeyIsUniqueWithinTenantAndIndependentAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        insertPreference(UUID.randomUUID(), tenantA, customerId);
        assertThatThrownBy(() -> insertPreference(UUID.randomUUID(), tenantA, customerId))
            .isInstanceOf(DuplicateKeyException.class);
        insertPreference(UUID.randomUUID(), tenantB, customerId);
        assertThat(jdbc.queryForObject("select count(*) from customer_notification_preference "
            + "where customer_id=?", Integer.class, customerId)).isEqualTo(2);
    }

    private void insertPreference(UUID id, UUID tenantId, UUID customerId) {
        jdbc.update("insert into customer_notification_preference "
                + "(id,tenant_id,customer_id,email_enabled,sms_enabled,created_at,updated_at,version) "
                + "values (?,?,?,true,false,current_timestamp,current_timestamp,0)",
            id, tenantId, customerId);
    }
}
