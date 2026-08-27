package com.transportlogistics.app.freight.loadplanning.adapters.outbound.fleet;

import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetLoadPlanLookupAdapterTest {

    private final FleetReportingQuery fleetReportingQuery = mock(FleetReportingQuery.class);
    private final FleetLoadPlanLookupAdapter adapter = new FleetLoadPlanLookupAdapter(fleetReportingQuery);

    @Test
    @DisplayName("Lookup vehicle returns complete authoritative capacity master facts")
    void lookupVehicleReturnsCapacityFacts() {
        var vehicleId = UUID.randomUUID();
        var summary = new FleetVehicleSummary(
                vehicleId,
                "WP-CAB-1234",
                "AVAILABLE",
                12000.0,
                5000.0,
                3500.0,
                8500.0,
                28.5,
                2,
                4500.0,
                true
        );

        when(fleetReportingQuery.findVehicle(vehicleId)).thenReturn(Optional.of(summary));

        var result = adapter.findVehicle(vehicleId);

        assertThat(result).isPresent();
        var view = result.get();
        assertThat(view.vehicleId()).isEqualTo(vehicleId);
        assertThat(view.registrationNumber()).isEqualTo("WP-CAB-1234");
        assertThat(view.capacityKg()).isEqualTo(5000.0);
        assertThat(view.tareWeightKg()).isEqualTo(3500.0);
        assertThat(view.grossVehicleWeightKg()).isEqualTo(8500.0);
        assertThat(view.cargoVolumeCapacityM3()).isEqualTo(28.5);
        assertThat(view.axleCount()).isEqualTo(2);
        assertThat(view.maxAxleLoadKg()).isEqualTo(4500.0);
        assertThat(view.operationalStatus()).isEqualTo("AVAILABLE");
        assertThat(view.active()).isTrue();
    }

    @Test
    @DisplayName("Lookup legacy vehicle preserves null capacity facts without default zeroing")
    void lookupLegacyVehiclePreservesNulls() {
        var vehicleId = UUID.randomUUID();
        var summary = new FleetVehicleSummary(
                vehicleId,
                "WP-LEGACY-01",
                "AVAILABLE",
                5000.0,
                true
        );

        when(fleetReportingQuery.findVehicle(vehicleId)).thenReturn(Optional.of(summary));

        var result = adapter.findVehicle(vehicleId);

        assertThat(result).isPresent();
        var view = result.get();
        assertThat(view.vehicleId()).isEqualTo(vehicleId);
        assertThat(view.capacityKg()).isNull();
        assertThat(view.tareWeightKg()).isNull();
        assertThat(view.grossVehicleWeightKg()).isNull();
        assertThat(view.cargoVolumeCapacityM3()).isNull();
        assertThat(view.axleCount()).isNull();
        assertThat(view.maxAxleLoadKg()).isNull();
    }
}
