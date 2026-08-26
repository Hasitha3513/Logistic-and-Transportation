package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueRequest(@NotNull UUID vehicleId,
                               UUID tripId,
                               UUID driverId,
                               @NotBlank @Size(max = 40) String fuelType,
                               @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
                               @DecimalMin(value = "0.0") BigDecimal unitPrice,
                               @NotNull UUID stationId,
                               @DecimalMin(value = "0.0") BigDecimal odometer,
                               @DecimalMin(value = "0.0") BigDecimal engineHours,
                               @NotNull OffsetDateTime issueDateTime,
                               @Size(max = 1000) String notes) {

    public FuelIssueUseCase.CreateCommand createCommand() {
        return new FuelIssueUseCase.CreateCommand(vehicleId, tripId, driverId, fuelType, quantity, unitPrice,
                stationId, odometer, engineHours, issueDateTime, notes);
    }

    public FuelIssueUseCase.UpdateCommand updateCommand() {
        return new FuelIssueUseCase.UpdateCommand(vehicleId, tripId, driverId, fuelType, quantity, unitPrice,
                stationId, odometer, engineHours, issueDateTime, notes);
    }
}
