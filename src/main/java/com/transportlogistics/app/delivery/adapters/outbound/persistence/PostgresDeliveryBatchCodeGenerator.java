package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchCodeGenerator;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Component
class PostgresDeliveryBatchCodeGenerator implements DeliveryBatchCodeGenerator {
    private final JdbcTemplate jdbc;

    PostgresDeliveryBatchCodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DeliveryBatchCode next(UUID tenantId, Year year) {
        Integer value;
        try {
            value = jdbc.queryForObject("""
                INSERT INTO delivery_batch_counter (tenant_id, calendar_year, last_value)
                VALUES (?, ?, 1)
                ON CONFLICT (tenant_id, calendar_year)
                DO UPDATE SET last_value = delivery_batch_counter.last_value + 1
                WHERE delivery_batch_counter.last_value < 999999
                RETURNING last_value
                """, Integer.class, tenantId, year.getValue());
        } catch (EmptyResultDataAccessException exhausted) {
            throw new BusinessRuleException("DELIVERY_BATCH_SEQUENCE_EXHAUSTED", "Delivery batch code sequence is exhausted for the Tenant year");
        } catch (org.springframework.dao.DataAccessException nonPg) {
            int updated = jdbc.update("""
                UPDATE delivery_batch_counter
                SET last_value = last_value + 1
                WHERE tenant_id = ? AND calendar_year = ? AND last_value < 999999
                """, tenantId, year.getValue());
            if (updated == 0) {
                Integer current = jdbc.queryForObject("""
                    SELECT MAX(last_value) FROM delivery_batch_counter
                    WHERE tenant_id = ? AND calendar_year = ?
                    """, Integer.class, tenantId, year.getValue());
                if (current != null && current >= 999999) {
                    throw new BusinessRuleException("DELIVERY_BATCH_SEQUENCE_EXHAUSTED", "Delivery batch code sequence is exhausted for the Tenant year");
                }
                try {
                    jdbc.update("""
                        INSERT INTO delivery_batch_counter (tenant_id, calendar_year, last_value)
                        VALUES (?, ?, 1)
                        """, tenantId, year.getValue());
                } catch (Exception ignored) {
                    jdbc.update("""
                        UPDATE delivery_batch_counter
                        SET last_value = last_value + 1
                        WHERE tenant_id = ? AND calendar_year = ?
                        """, tenantId, year.getValue());
                }
            }
            value = jdbc.queryForObject("""
                SELECT last_value FROM delivery_batch_counter
                WHERE tenant_id = ? AND calendar_year = ?
                """, Integer.class, tenantId, year.getValue());
        }
        return new DeliveryBatchCode(String.format("BAT-%d-%06d", year.getValue(), value != null ? value : 1));
    }
}
