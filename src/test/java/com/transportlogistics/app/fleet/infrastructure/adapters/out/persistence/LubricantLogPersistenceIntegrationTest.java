package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LubricantLogPersistenceAdapter.class)
class LubricantLogPersistenceIntegrationTest {

    @Autowired
    private LubricantLogPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Should save and retrieve vehicle lubricant logs")
    void shouldSaveAndRetrieveLubricantLog() {
        var vehicleId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        jdbc.update("INSERT INTO vehicle_category (id, code, name, active) VALUES (?, ?, ?, ?)",
                categoryId, "CAT-" + vehicleId.toString().substring(0, 6), "Test Category", true);
        jdbc.update("INSERT INTO vehicle_type (id, category_id, code, name, active) VALUES (?, ?, ?, ?, ?)",
                typeId, categoryId, "TYPE-" + vehicleId.toString().substring(0, 6), "Test Type", true);
        jdbc.update("INSERT INTO vehicle (id, registration_number, chassis_number, engine_number, category_id, " +
                        "type_id, manufacturer, model, manufacture_year, ownership_type, operational_status, " +
                        "current_odometer_km, engine_hours, capacity_kg, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                vehicleId, "WP-LUB-" + vehicleId.toString().substring(0, 6), "VIN-LUB", "ENG-LUB",
                categoryId, typeId, "Toyota", "Dyna", 2021, "COMPANY_OWNED", "AVAILABLE",
                25000.0, 500.0, 3000.0, true);

        var now = OffsetDateTime.now();
        var log = new LubricantLog(
                UUID.randomUUID(), vehicleId, FluidType.ENGINE_OIL, new BigDecimal("8.50"), MeasurementUnit.LITRE,
                now, 25000.0, 500.0, null, "Caltex", "REF-LUB-01", "Engine oil top up",
                true, now, now, "tester", "tester"
        );

        var saved = adapter.save(log);
        assertNotNull(saved);

        var list = adapter.findByVehicleId(vehicleId);
        assertEquals(1, list.size());
        assertEquals(FluidType.ENGINE_OIL, list.get(0).fluidType());
        assertEquals(new BigDecimal("8.50"), list.get(0).quantity());
        assertEquals("REF-LUB-01", list.get(0).referenceNumber());

        var filtered = adapter.findByVehicleIdWithFilters(vehicleId, FluidType.ENGINE_OIL, now.minusHours(1), now.plusHours(1));
        assertEquals(1, filtered.size());

        var emptyFilter = adapter.findByVehicleIdWithFilters(vehicleId, FluidType.COOLANT, null, null);
        assertTrue(emptyFilter.isEmpty());
    }
}
