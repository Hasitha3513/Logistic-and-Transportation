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
    UUID id;
    @Column(name = "trip_number")
    String tripNumber;
    @Column(name = "customer_id")
    UUID customerId;
    @Column(name = "department_id")
    UUID departmentId;
    @Column(name = "project_id")
    UUID projectId;
    @Column(name = "route_id")
    UUID routeId;
    String priority;
    String status;
    @Column(name = "origin_location_id")
    UUID originLocationId;
    @Column(name = "destination_location_id")
    UUID destinationLocationId;
    @Column(name = "requested_start_time")
    OffsetDateTime requestedStartTime;
    @Column(name = "requested_end_time")
    OffsetDateTime requestedEndTime;
    @Column(name = "required_vehicle_type_id")
    UUID requiredVehicleTypeId;
    @Column(name = "required_capacity_kg")
    Double requiredCapacityKg;
    @Column(name = "cargo_description")
    String cargoDescription;
    @Column(name = "passenger_count")
    Integer passengerCount;
    @Column(name = "customer_instructions")
    String customerInstructions;
    String notes;
    @Column(name = "vehicle_id")
    UUID vehicleId;
    @Column(name = "driver_id")
    UUID driverId;
    @Column(name = "actual_start_time")
    OffsetDateTime actualStartTime;
    @Column(name = "actual_end_time")
    OffsetDateTime actualEndTime;
    @Column(name = "start_odometer_km")
    Double startOdometerKm;
    @Column(name = "end_odometer_km")
    Double endOdometerKm;
    @Column(name = "completion_remarks")
    String completionRemarks;
    @Column(name = "created_at")
    OffsetDateTime createdAt;
    @Column(name = "updated_at")
    OffsetDateTime updatedAt;
}