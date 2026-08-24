package com.transportlogistics.app.fleet;

import java.time.LocalDate;
import java.util.UUID;

public record FleetDocumentAlert(
        UUID id,
        String documentType,
        String documentNumber,
        String registrationNumber,
        LocalDate expiryDate,
        String severity
) {
}
