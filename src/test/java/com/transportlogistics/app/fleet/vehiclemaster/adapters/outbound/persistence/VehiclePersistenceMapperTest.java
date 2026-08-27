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
                "OUT_OF_SERVICE", 12_000.0, 450.0, 5_000.0,
                3200.0, 8200.0, 24.5, 2, 4200.0, true);

        var entity = mapper.toEntity(vehicle);
        var restored = mapper.toDomain(entity);

        assertThat(restored).isEqualTo(vehicle);
        assertThat(restored.tareWeightKg()).isEqualTo(3200.0);
        assertThat(restored.grossVehicleWeightKg()).isEqualTo(8200.0);
        assertThat(restored.cargoVolumeCapacityM3()).isEqualTo(24.5);
        assertThat(restored.axleCount()).isEqualTo(2);
        assertThat(restored.maxAxleLoadKg()).isEqualTo(4200.0);
    }

    @Test
    void mapsLegacyVehicleWithNullCapacityFields() {
        var vehicle = new Vehicle(UUID.randomUUID(), "WP-CAB-1201", "CH-111", "EN-222",
                UUID.randomUUID(), UUID.randomUUID(), "Isuzu", "NPR", 2021, "RENTED",
                "AVAILABLE", 12_000.0, 450.0, 5_000.0, true);

        var entity = mapper.toEntity(vehicle);
        var restored = mapper.toDomain(entity);

        assertThat(restored).isEqualTo(vehicle);
        assertThat(restored.tareWeightKg()).isNull();
        assertThat(restored.grossVehicleWeightKg()).isNull();
        assertThat(restored.cargoVolumeCapacityM3()).isNull();
        assertThat(restored.axleCount()).isNull();
        assertThat(restored.maxAxleLoadKg()).isNull();
    }
}
