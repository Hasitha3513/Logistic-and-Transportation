package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class VehicleEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "registration_number")
    private String registrationNumber;
    @Column(name = "chassis_number")
    private String chassisNumber;
    @Column(name = "engine_number")
    private String engineNumber;
    @Column(name = "category_id")
    private UUID categoryId;
    @Column(name = "type_id")
    private UUID typeId;
    @Column(name = "manufacturer")
    private String manufacturer;
    @Column(name = "model")
    private String model;
    @Column(name = "manufacture_year")
    private Integer manufactureYear;
    @Column(name = "ownership_type")
    private String ownershipType;
    @Column(name = "operational_status")
    private String operationalStatus;
    @Column(name = "current_odometer_km")
    private Double currentOdometerKm;
    @Column(name = "engine_hours")
    private Double engineHours;
    @Column(name = "capacity_kg")
    private Double capacityKg;
    @Column(name = "tare_weight_kg")
    private Double tareWeightKg;
    @Column(name = "gross_vehicle_weight_kg")
    private Double grossVehicleWeightKg;
    @Column(name = "cargo_volume_capacity_m3")
    private Double cargoVolumeCapacityM3;
    @Column(name = "axle_count")
    private Integer axleCount;
    @Column(name = "max_axle_load_kg")
    private Double maxAxleLoadKg;
    @Column(name = "active")
    private boolean active;

    public VehicleEntity() {}

    public VehicleEntity(UUID id, String registrationNumber, String chassisNumber, String engineNumber,
                         UUID categoryId, UUID typeId, String manufacturer, String model,
                         Integer manufactureYear, String ownershipType, String operationalStatus,
                         Double currentOdometerKm, Double engineHours, Double capacityKg, boolean active) {
        this(id, registrationNumber, chassisNumber, engineNumber, categoryId, typeId, manufacturer, model,
                manufactureYear, ownershipType, operationalStatus, currentOdometerKm, engineHours, capacityKg,
                null, null, null, null, null, active);
    }

    public VehicleEntity(UUID id, String registrationNumber, String chassisNumber, String engineNumber,
                         UUID categoryId, UUID typeId, String manufacturer, String model,
                         Integer manufactureYear, String ownershipType, String operationalStatus,
                         Double currentOdometerKm, Double engineHours, Double capacityKg,
                         Double tareWeightKg, Double grossVehicleWeightKg, Double cargoVolumeCapacityM3,
                         Integer axleCount, Double maxAxleLoadKg, boolean active) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.chassisNumber = chassisNumber;
        this.engineNumber = engineNumber;
        this.categoryId = categoryId;
        this.typeId = typeId;
        this.manufacturer = manufacturer;
        this.model = model;
        this.manufactureYear = manufactureYear;
        this.ownershipType = ownershipType;
        this.operationalStatus = operationalStatus;
        this.currentOdometerKm = currentOdometerKm;
        this.engineHours = engineHours;
        this.capacityKg = capacityKg;
        this.tareWeightKg = tareWeightKg;
        this.grossVehicleWeightKg = grossVehicleWeightKg;
        this.cargoVolumeCapacityM3 = cargoVolumeCapacityM3;
        this.axleCount = axleCount;
        this.maxAxleLoadKg = maxAxleLoadKg;
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getChassisNumber() { return chassisNumber; }
    public void setChassisNumber(String chassisNumber) { this.chassisNumber = chassisNumber; }

    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public UUID getTypeId() { return typeId; }
    public void setTypeId(UUID typeId) { this.typeId = typeId; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(Integer manufactureYear) { this.manufactureYear = manufactureYear; }

    public String getOwnershipType() { return ownershipType; }
    public void setOwnershipType(String ownershipType) { this.ownershipType = ownershipType; }

    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }

    public Double getCurrentOdometerKm() { return currentOdometerKm; }
    public void setCurrentOdometerKm(Double currentOdometerKm) { this.currentOdometerKm = currentOdometerKm; }

    public Double getEngineHours() { return engineHours; }
    public void setEngineHours(Double engineHours) { this.engineHours = engineHours; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public Double getTareWeightKg() { return tareWeightKg; }
    public void setTareWeightKg(Double tareWeightKg) { this.tareWeightKg = tareWeightKg; }

    public Double getGrossVehicleWeightKg() { return grossVehicleWeightKg; }
    public void setGrossVehicleWeightKg(Double grossVehicleWeightKg) { this.grossVehicleWeightKg = grossVehicleWeightKg; }

    public Double getCargoVolumeCapacityM3() { return cargoVolumeCapacityM3; }
    public void setCargoVolumeCapacityM3(Double cargoVolumeCapacityM3) { this.cargoVolumeCapacityM3 = cargoVolumeCapacityM3; }

    public Integer getAxleCount() { return axleCount; }
    public void setAxleCount(Integer axleCount) { this.axleCount = axleCount; }

    public Double getMaxAxleLoadKg() { return maxAxleLoadKg; }
    public void setMaxAxleLoadKg(Double maxAxleLoadKg) { this.maxAxleLoadKg = maxAxleLoadKg; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
