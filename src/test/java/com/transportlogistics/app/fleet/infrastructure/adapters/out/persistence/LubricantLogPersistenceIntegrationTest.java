package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({LubricantLogPersistenceAdapter.class, VehiclePersistenceAdapter.class})
class LubricantLogPersistenceIntegrationTest {

    @Autowired
    private LubricantLogPersistenceAdapter adapter;

    @Autowired
    private VehiclePersistenceAdapter vehicleAdapter;

    @Test
    @DisplayName("Should save and retrieve vehicle lubricant logs")
    void shouldSaveAndRetrieveLubricantLog() {
        var vehicleId = UUID.randomUUID();
        var vehicle = new Vehicle(vehicleId, "WP-LUB-" + vehicleId.toString().substring(0, 6), "VIN-LUB", "ENG-LUB",
                UUID.randomUUID(), UUID.randomUUID(), "Toyota", "Dyna", 2021, "COMPANY_OWNED", "AVAILABLE",
                25000.0, 500.0, 3000.0, true);
        vehicleAdapter.save(vehicle);

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
