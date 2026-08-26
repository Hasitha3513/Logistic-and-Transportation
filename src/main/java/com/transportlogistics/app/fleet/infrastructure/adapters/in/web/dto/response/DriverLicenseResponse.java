package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverLicenseResponse(UUID id,
                                    UUID driverId,
                                    String licenseNumber,
                                    String licenseClass,
                                    LocalDate issueDate,
                                    LocalDate expiryDate,
                                    DriverLicenseStatus status,
                                    boolean active,
                                    OffsetDateTime createdAt,
                                    OffsetDateTime updatedAt,
                                    String createdBy,
                                    String updatedBy) {
}
