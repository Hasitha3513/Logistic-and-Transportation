package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DriverAssignmentRequest(@NotNull UUID driverId, @NotBlank String requiredLicenseClass) {
}
