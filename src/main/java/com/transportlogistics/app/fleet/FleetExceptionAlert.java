package com.transportlogistics.app.fleet;

import java.util.UUID;

public record FleetExceptionAlert(
        UUID id,
        String exceptionType,
        String driverName,
        String severity,
        String status,
        String reason
) {
}
