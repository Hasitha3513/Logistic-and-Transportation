package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor
class TripEntity {
    @Id
    private UUID id;
    @Column(name = "trip_number")
    private String tripNumber;
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "department_id")
    private UUID departmentId;
    @Column(name = "project_id")
    private UUID projectId;
    @Column(name = "route_id")
    private UUID routeId;
    private String priority;
    private String status;
    @Column(name = "origin_location_id")
    private UUID originLocationId;
    @Column(name = "destination_location_id")
    private UUID destinationLocationId;
    @Column(name = "requested_start_time")
    private OffsetDateTime requestedStartTime;
    @Column(name = "requested_end_time")
    private OffsetDateTime requestedEndTime;
    @Column(name = "required_vehicle_type_id")
    private UUID requiredVehicleTypeId;
    @Column(name = "required_capacity_kg")
    private Double requiredCapacityKg;
    @Column(name = "cargo_description")
    private String cargoDescription;
    @Column(name = "passenger_count")
    private Integer passengerCount;
    @Column(name = "customer_instructions")
    private String customerInstructions;
    private String notes;
    @Column(name = "vehicle_id")
    private UUID vehicleId;
    @Column(name = "driver_id")
    private UUID driverId;
    @Column(name = "actual_start_time")
    private OffsetDateTime actualStartTime;
    @Column(name = "actual_end_time")
    private OffsetDateTime actualEndTime;
    @Column(name = "start_odometer_km")
    private Double startOdometerKm;
    @Column(name = "end_odometer_km")
    private Double endOdometerKm;
    @Column(name = "completion_remarks")
    private String completionRemarks;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}