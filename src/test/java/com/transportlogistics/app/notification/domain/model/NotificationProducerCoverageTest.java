package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationProducerCoverageTest {
    private static final Set<String> PRODUCTION_PRODUCERS = Set.of(
        "TRIP_DELAY_RECORDED",
        "TRIP_INCIDENT_RECORDED",
        "VEHICLE_MAINTENANCE_DUE",
        "VEHICLE_DOCUMENT_EXPIRING",
        "DRIVER_EXCEPTION_RECORDED",
        "DRIVER_MEDICAL_EXPIRING",
        "DRIVER_DRUG_TEST_FAILED",
        "DRIVER_LICENSE_EXPIRING"
    );

    @Test void everyAndOnlyFrozenMvpCatalogueEventHasProductionProducerEvidence() {
        var catalogueEvents = NotificationEventCatalogue.all().stream()
            .map(NotificationEventDefinition::eventType).collect(Collectors.toSet());
        assertThat(catalogueEvents).hasSize(8).isEqualTo(PRODUCTION_PRODUCERS);
        assertThat(catalogueEvents).doesNotContain("DRIVER_DRUG_TEST_EXPIRING", "FUEL_LIMIT_EXCEEDED", "FUEL_EXCEPTION");
    }
}
