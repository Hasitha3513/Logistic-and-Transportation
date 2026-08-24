package com.transportlogistics.app.fleet.domain.model;

import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleDomainTest {

    @Test
    @DisplayName("Successfully instantiate Vehicle with full valid fields")
    void fullVehicleValid() {
        var id = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var vehicle = new Vehicle(id, " WP-CAB-1234 ", " CH-987654 ", " ENG-123456 ",
                categoryId, typeId, "Toyota", "Dyna", 2022, "COMPANY_OWNED",
                "AVAILABLE", 15000.0, 350.0, 4500.0, true);

        assertThat(vehicle.id()).isEqualTo(id);
        assertThat(vehicle.registrationNumber()).isEqualTo("WP-CAB-1234");
        assertThat(vehicle.chassisNumber()).isEqualTo("CH-987654");
        assertThat(vehicle.engineNumber()).isEqualTo("ENG-123456");
        assertThat(vehicle.categoryId()).isEqualTo(categoryId);
        assertThat(vehicle.typeId()).isEqualTo(typeId);
        assertThat(vehicle.manufacturer()).isEqualTo("Toyota");
        assertThat(vehicle.model()).isEqualTo("Dyna");
        assertThat(vehicle.manufactureYear()).isEqualTo(2022);
        assertThat(vehicle.ownershipType()).isEqualTo("COMPANY_OWNED");
        assertThat(vehicle.operationalStatus()).isEqualTo("AVAILABLE");
        assertThat(vehicle.currentOdometerKm()).isEqualTo(15000.0);
        assertThat(vehicle.engineHours()).isEqualTo(350.0);
        assertThat(vehicle.capacityKg()).isEqualTo(4500.0);
        assertThat(vehicle.active()).isTrue();
    }

    @Test
    @DisplayName("Default and normalize optional and required fields")
    void minimalVehicleDefaults() {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var vehicle = new Vehicle(null, "wp-cab-9999", " ", "", categoryId, typeId,
                null, null, null, null, null, null, null, null, true);

        assertThat(vehicle.id()).isNotNull();
        assertThat(vehicle.registrationNumber()).isEqualTo("WP-CAB-9999");
        assertThat(vehicle.chassisNumber()).isNull();
        assertThat(vehicle.engineNumber()).isNull();
        assertThat(vehicle.ownershipType()).isEqualTo("COMPANY_OWNED");
        assertThat(vehicle.operationalStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Reject blank or null registration number")
    void rejectInvalidRegistration() {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), null, null, null, categoryId, typeId,
                null, null, null, null, null, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Registration number is required");

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "   ", null, null, categoryId, typeId,
                null, null, null, null, null, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Registration number is required");
    }

    @Test
    @DisplayName("Reject negative capacity, odometer, and engine hours")
    void rejectNegativeMeters() {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "WP-CAB-1234", null, null, categoryId, typeId,
                null, null, null, null, null, null, null, -10.0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity cannot be negative");

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "WP-CAB-1234", null, null, categoryId, typeId,
                null, null, null, null, null, -1.0, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current odometer cannot be negative");

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "WP-CAB-1234", null, null, categoryId, typeId,
                null, null, null, null, null, null, -5.0, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Engine hours cannot be negative");
    }

    @Test
    @DisplayName("Reject manufacture year out of realistic bounds")
    void rejectInvalidManufactureYear() {
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "WP-CAB-1234", null, null, categoryId, typeId,
                null, null, 1899, null, null, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manufacture year must be between 1900 and");

        assertThatThrownBy(() -> new Vehicle(UUID.randomUUID(), "WP-CAB-1234", null, null, categoryId, typeId,
                null, null, Year.now().getValue() + 5, null, null, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manufacture year must be between 1900 and");
    }

    @Test
    @DisplayName("Validate allowed operational status transitions")
    void validStatusTransitions() {
        Vehicle.validateStatusTransition("AVAILABLE", "AVAILABLE");
        Vehicle.validateStatusTransition("AVAILABLE", "ALLOCATED");
        Vehicle.validateStatusTransition("AVAILABLE", "MAINTENANCE");
        Vehicle.validateStatusTransition("AVAILABLE", "OUT_OF_SERVICE");
        Vehicle.validateStatusTransition("AVAILABLE", "BROKEN_DOWN");

        Vehicle.validateStatusTransition("ALLOCATED", "AVAILABLE");
        Vehicle.validateStatusTransition("ALLOCATED", "MAINTENANCE");
        Vehicle.validateStatusTransition("ALLOCATED", "OUT_OF_SERVICE");
        Vehicle.validateStatusTransition("ALLOCATED", "BROKEN_DOWN");

        Vehicle.validateStatusTransition("MAINTENANCE", "AVAILABLE");
        Vehicle.validateStatusTransition("MAINTENANCE", "OUT_OF_SERVICE");

        Vehicle.validateStatusTransition("OUT_OF_SERVICE", "AVAILABLE");
        Vehicle.validateStatusTransition("OUT_OF_SERVICE", "MAINTENANCE");
    }

    @Test
    @DisplayName("Reject invalid status transitions with 409 ConflictException")
    void invalidStatusTransitions() {
        assertThatThrownBy(() -> Vehicle.validateStatusTransition("MAINTENANCE", "ALLOCATED"))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    var ce = (ConflictException) e;
                    assertThat(ce.code()).isEqualTo("VEHICLE_STATUS_TRANSITION_INVALID");
                });

        assertThatThrownBy(() -> Vehicle.validateStatusTransition("OUT_OF_SERVICE", "ALLOCATED"))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    var ce = (ConflictException) e;
                    assertThat(ce.code()).isEqualTo("VEHICLE_STATUS_TRANSITION_INVALID");
                });
    }
}
