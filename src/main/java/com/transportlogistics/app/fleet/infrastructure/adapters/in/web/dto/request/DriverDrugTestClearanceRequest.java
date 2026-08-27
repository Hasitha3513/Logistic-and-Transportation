package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import java.time.OffsetDateTime;

public record DriverDrugTestClearanceRequest(
        OffsetDateTime clearedAt,
        String remarks
) {}
