package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vehicle")
@Getter
@Setter
@NoArgsConstructor
class VehicleEntity {
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
    @Column(name = "active")
    private boolean active;

    public VehicleEntity(UUID id, String registrationNumber, String chassisNumber, String engineNumber,
                         UUID categoryId, UUID typeId, String manufacturer, String model,
                         Integer manufactureYear, String ownershipType, String operationalStatus,
                         Double currentOdometerKm, Double engineHours, Double capacityKg, boolean active) {
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
        this.active = active;
    }
}
