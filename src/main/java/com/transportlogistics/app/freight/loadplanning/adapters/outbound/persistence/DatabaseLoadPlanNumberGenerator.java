package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class DatabaseLoadPlanNumberGenerator implements LoadPlanNumberGenerator {

    private final JdbcTemplate jdbc;

    public DatabaseLoadPlanNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String next() {
        Long value = jdbc.queryForObject("SELECT nextval('load_plan_number_sequence')", Long.class);
        return "LP-%04d-%06d".formatted(Year.now().getValue(), value != null ? value : 1L);
    }
}
