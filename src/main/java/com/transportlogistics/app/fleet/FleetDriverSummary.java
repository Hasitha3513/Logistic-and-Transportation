package com.transportlogistics.app.fleet;

import java.util.UUID;

public record FleetDriverSummary(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String status,
        boolean active
) {
}
