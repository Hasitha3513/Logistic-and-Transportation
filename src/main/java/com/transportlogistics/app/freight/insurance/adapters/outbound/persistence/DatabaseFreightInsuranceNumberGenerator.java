package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class DatabaseFreightInsuranceNumberGenerator implements FreightInsuranceNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFreightInsuranceNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String nextPolicyNumber() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('freight_policy_number_sequence')", Long.class);
        return String.format("POL-%d-%06d", Year.now().getValue(), nextVal != null ? nextVal : 1L);
    }

    @Override
    public String nextClaimNumber() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('freight_claim_number_sequence')", Long.class);
        return String.format("CLM-%d-%06d", Year.now().getValue(), nextVal != null ? nextVal : 1L);
    }
}
