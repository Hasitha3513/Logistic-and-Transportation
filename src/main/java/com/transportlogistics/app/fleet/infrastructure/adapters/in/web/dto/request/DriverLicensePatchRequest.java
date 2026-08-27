package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;

import java.time.LocalDate;

public record DriverLicensePatchRequest(String licenseNumber,
                                        String licenseClass,
                                        LocalDate issueDate,
                                        LocalDate expiryDate,
                                        DriverLicenseStatus status,
                                        Boolean active) {
}
