package com.transportlogistics.app.freight.loadplanning.adapters.outbound.fleet;

import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.fleet.FleetVehicleSummary;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.VehicleLoadSpaceLookupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FleetLoadPlanLookupAdapter implements VehicleLoadSpaceLookupPort {

    private final FleetReportingQuery fleetReportingQuery;

    public FleetLoadPlanLookupAdapter(FleetReportingQuery fleetReportingQuery) {
        this.fleetReportingQuery = fleetReportingQuery;
    }

    @Override
    public Optional<VehiclePlanningView> findVehicle(UUID vehicleId) {
        return fleetReportingQuery.findVehicle(vehicleId)
                .map(this::toVehicleView);
    }

    private VehiclePlanningView toVehicleView(FleetVehicleSummary summary) {
        return new VehiclePlanningView(
                summary.id(),
                summary.registrationNumber(),
                summary.capacityKg(),
                summary.tareWeightKg(),
                summary.grossVehicleWeightKg(),
                summary.cargoVolumeCapacityM3(),
                summary.axleCount(),
                summary.maxAxleLoadKg(),
                summary.operationalStatus(),
                summary.active()
        );
    }
}
