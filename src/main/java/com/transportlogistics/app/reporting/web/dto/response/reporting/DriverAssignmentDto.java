package com.transportlogistics.app.reporting.web.dto.response.reporting;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverAssignmentDto {
    private UUID driverId;
    private String employeeNumber;
    private UUID tripId;
    private String tripNumber;
    private String status;
    private LocalDate scheduledDeparture;
    private LocalDate scheduledArrival;
    private UUID vehicleId;
    private String vehiclePlateNumber;
    private UUID routeId;
    private Double distanceKm;
    private LocalDate assignmentTimestamp;
}
