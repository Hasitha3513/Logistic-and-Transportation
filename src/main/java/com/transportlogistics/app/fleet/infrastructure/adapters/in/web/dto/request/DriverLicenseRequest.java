package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DriverLicenseRequest(@NotBlank String licenseNumber,
                                   @NotBlank String licenseClass,
                                   @NotNull LocalDate issueDate,
                                   @NotNull LocalDate expiryDate,
                                   @NotNull DriverLicenseStatus status,
                                   Boolean active) {
}
