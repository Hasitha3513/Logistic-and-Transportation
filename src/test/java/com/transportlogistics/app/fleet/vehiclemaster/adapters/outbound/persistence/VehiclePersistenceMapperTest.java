package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VehiclePersistenceMapperTest {

    private final VehiclePersistenceMapper mapper = new VehiclePersistenceMapper();

    @Test
    void mapsVehicleToJpaEntityAndBackWithoutLosingState() {
        var vehicle = new Vehicle(UUID.randomUUID(), "WP-CAB-1201", "CH-111", "EN-222",
                UUID.randomUUID(), UUID.randomUUID(), "Isuzu", "NPR", 2021, "RENTED",
                "OUT_OF_SERVICE", 12_000.0, 450.0, 5_000.0, true);

        var entity = mapper.toEntity(vehicle);
        var restored = mapper.toDomain(entity);

        assertThat(restored).isEqualTo(vehicle);
    }
}
