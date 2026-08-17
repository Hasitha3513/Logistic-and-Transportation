package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
class DatabaseFuelVoucherGenerator implements FuelVoucherGenerator {
    private final JdbcTemplate jdbc;

    @Override
    public String next(int year) {
        var sequence = jdbc.queryForObject("SELECT nextval('fuel_voucher_sequence')", Long.class);
        return String.format(Locale.ROOT, "FUEL-%04d-%06d", year, sequence);
    }
}
