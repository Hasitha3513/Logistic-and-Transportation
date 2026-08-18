package com.transportlogistics.app.reporting.web.dto.response.reporting;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripReportDto {
    private UUID tripId;
    private String tripNumber;
    private String status;
    private LocalDate scheduledDeparture;
    private LocalDate scheduledArrival;
    private LocalDate actualStart;
    private LocalDate actualCompletion;
    private UUID vehicleId;
    private String vehiclePlateNumber;
    private UUID driverId;
    private String driverEmployeeNumber;
    private UUID routeId;
    private Double distanceKm;
    private UUID customerId;
}
