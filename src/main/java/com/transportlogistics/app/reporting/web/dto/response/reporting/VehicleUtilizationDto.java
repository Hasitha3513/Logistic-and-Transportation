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
public class VehicleUtilizationDto {
    private UUID vehicleId;
    private String plateNumber;
    private long tripCount;
    private long allocatedDurationMinutes;
    private long completedTripCount;
    private double distanceTravelledKm;
    private String operationalStatus;
}
