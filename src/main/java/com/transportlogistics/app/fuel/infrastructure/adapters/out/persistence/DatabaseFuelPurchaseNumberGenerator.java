package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
@RequiredArgsConstructor
class DatabaseFuelPurchaseNumberGenerator implements FuelPurchaseNumberGenerator {
    private final JdbcTemplate jdbc;

    @Override
    public String next(LocalDate date) {
        Long sequence = jdbc.queryForObject("SELECT nextval('fuel_purchase_number_sequence')", Long.class);
        return String.format(Locale.ROOT, "FP-%04d-%06d", date == null ? LocalDate.now().getYear() : date.getYear(), sequence);
    }
}
