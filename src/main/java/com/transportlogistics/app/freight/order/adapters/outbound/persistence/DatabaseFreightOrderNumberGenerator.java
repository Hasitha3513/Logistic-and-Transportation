package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
class DatabaseFreightOrderNumberGenerator implements FreightOrderNumberGenerator {
    private final JdbcTemplate jdbc;
    DatabaseFreightOrderNumberGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public String next(OffsetDateTime pickupAt) {
        Long sequence = jdbc.queryForObject("SELECT nextval('freight_order_number_sequence')", Long.class);
        int year = pickupAt == null ? OffsetDateTime.now().getYear() : pickupAt.getYear();
        return "FO-%04d-%06d".formatted(year, sequence);
    }
}
