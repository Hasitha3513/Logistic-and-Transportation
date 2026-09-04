package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverLookup;
import com.transportlogistics.app.fleet.VehicleFuelContextLookup;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardReferencePort;
import com.transportlogistics.app.organization.VendorLookup;
import com.transportlogistics.app.trip.TripFuelContextLookup;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseRepository;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
class FuelCardReferenceAdapter implements FuelCardReferencePort {
    private final VendorLookup providers; private final VehicleFuelContextLookup vehicles; private final DriverLookup drivers;
    private final TripFuelContextLookup trips; private final FuelPurchaseRepository purchases;
    FuelCardReferenceAdapter(VendorLookup providers, VehicleFuelContextLookup vehicles, DriverLookup drivers,
                             TripFuelContextLookup trips, FuelPurchaseRepository purchases) {
        this.providers=providers; this.vehicles=vehicles; this.drivers=drivers; this.trips=trips; this.purchases=purchases;
    }
    @Override public boolean providerActive(UUID id){return providers.find(id).map(VendorLookup.VendorReference::active).orElse(false);}
    @Override public boolean vehicleActive(UUID id){return vehicles.find(id).map(VehicleFuelContextLookup.VehicleFuelContext::active).orElse(false);}
    @Override public boolean driverActive(UUID id){return drivers.findDriver(id).map(com.transportlogistics.app.fleet.FleetDriverSummary::active).orElse(false);}
    @Override public boolean tripExists(UUID id){return trips.find(id).isPresent();}
    @Override public boolean purchaseExists(UUID id){return purchases.findById(id).isPresent();}
}
