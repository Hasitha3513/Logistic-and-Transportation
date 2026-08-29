package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryNumberGenerator;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Component
class PostgresDeliveryNumberGenerator implements DeliveryNumberGenerator {
    private final JdbcTemplate jdbc;
    PostgresDeliveryNumberGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public DeliveryNumber next(UUID tenantId, Year year) {
        Integer value;
        try {
            value = jdbc.queryForObject("""
                INSERT INTO delivery_number_counter (tenant_id, calendar_year, last_value)
                VALUES (?, ?, 1)
                ON CONFLICT (tenant_id, calendar_year)
                DO UPDATE SET last_value = delivery_number_counter.last_value + 1
                WHERE delivery_number_counter.last_value < 999999
                RETURNING last_value
                    """, Integer.class, tenantId, year.getValue());
        } catch (EmptyResultDataAccessException exhausted) {
            throw new BusinessRuleException("DELIVERY_NUMBER_SEQUENCE_EXHAUSTED", "Delivery number sequence is exhausted for the Tenant year");
        }
        if (value == null || value > 999999) {
            throw new BusinessRuleException("DELIVERY_NUMBER_SEQUENCE_EXHAUSTED", "Delivery number sequence is exhausted for the Tenant year");
        }
        return new DeliveryNumber("DEL-%04d-%06d".formatted(year.getValue(), value));
    }
}
