package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
class VehiclePersistenceMapper {

    VehicleEntity toEntity(Vehicle vehicle) {
        return new VehicleEntity(vehicle.id(), vehicle.registrationNumber(), vehicle.chassisNumber(),
                vehicle.engineNumber(), vehicle.categoryId(), vehicle.typeId(), vehicle.manufacturer(),
                vehicle.model(), vehicle.manufactureYear(), vehicle.ownershipType(), vehicle.operationalStatus(),
                vehicle.currentOdometerKm(), vehicle.engineHours(), vehicle.capacityKg(),
                vehicle.tareWeightKg(), vehicle.grossVehicleWeightKg(), vehicle.cargoVolumeCapacityM3(),
                vehicle.axleCount(), vehicle.maxAxleLoadKg(), vehicle.active());
    }

    Vehicle toDomain(VehicleEntity entity) {
        return new Vehicle(entity.getId(), entity.getRegistrationNumber(), entity.getChassisNumber(),
                entity.getEngineNumber(), entity.getCategoryId(), entity.getTypeId(), entity.getManufacturer(),
                entity.getModel(), entity.getManufactureYear(), entity.getOwnershipType(),
                entity.getOperationalStatus(), entity.getCurrentOdometerKm(), entity.getEngineHours(),
                entity.getCapacityKg(), entity.getTareWeightKg(), entity.getGrossVehicleWeightKg(),
                entity.getCargoVolumeCapacityM3(), entity.getAxleCount(), entity.getMaxAxleLoadKg(),
                entity.isActive());
    }
}
