package com.transportlogistics.app.compliance.ports.outbound;

import com.transportlogistics.app.compliance.domain.ComplianceSourceFactReference;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CargoHazmatFactsQuery {

    List<CargoHazmatFact> findAt(UUID tenantId, UUID manifestId, Instant effectiveAt);

    record CargoHazmatFact(
            ComplianceSourceFactReference reference,
            String commodityClassification,
            boolean customsApplicable,
            boolean customsInformationPresent,
            boolean hazardous,
            String hazardousClassification,
            boolean hazardousDetailsPresent) {
    }
}
