package com.transportlogistics.app.compliance.ports.outbound;

import com.transportlogistics.app.compliance.domain.ComplianceSourceFactReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VehicleDocumentFactsQuery {

    List<VehicleDocumentFact> findAt(UUID tenantId, UUID vehicleId, Instant effectiveAt);

    record VehicleDocumentFact(
            ComplianceSourceFactReference reference,
            String documentType,
            boolean active,
            boolean mandatoryForDispatch,
            LocalDate validFrom,
            LocalDate validTo) {
    }
}
