package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelIssueResponse(UUID id,
                                String voucherNumber,
                                Reference vehicle,
                                Reference trip,
                                Reference driver,
                                String fuelType,
                                BigDecimal quantity,
                                BigDecimal unitPrice,
                                BigDecimal totalAmount,
                                FuelStationResponse station,
                                BigDecimal odometer,
                                BigDecimal engineHours,
                                OffsetDateTime issueDateTime,
                                FuelIssueStatus status,
                                UUID requestedBy,
                                UUID authorizedBy,
                                OffsetDateTime authorizationDateTime,
                                String notes,
                                OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
}
