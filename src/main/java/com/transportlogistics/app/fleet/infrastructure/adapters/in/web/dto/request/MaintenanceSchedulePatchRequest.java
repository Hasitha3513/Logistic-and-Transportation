package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MaintenanceSchedulePatchRequest(
        String maintenanceType,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        MaintenanceStatus status,
        String description,
        String serviceProvider,
        BigDecimal cost
) {
}
