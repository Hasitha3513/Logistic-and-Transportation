package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class DatabaseCargoExceptionNumberGenerator implements CargoExceptionNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCargoExceptionNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String nextExceptionNumber() {
        Long nextVal = jdbcTemplate.queryForObject(
                "SELECT nextval('cargo_exception_number_sequence')", Long.class);
        return String.format("CEX-%d-%06d", Year.now().getValue(), nextVal != null ? nextVal : 1L);
    }
}
