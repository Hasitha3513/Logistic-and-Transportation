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
        "DRIVER_LICENSE_EXPIRING",
        "DELIVERY_OUT_FOR_DELIVERY",
        "DELIVERY_ETA_RISK_CHANGED",
        "DELIVERY_COMPLETED",
        "DELIVERY_FAILED_ATTEMPT_RECORDED",
        "DELIVERY_REDELIVERY_SCHEDULED",
        "VEHICLE_GEOFENCE_TRANSITIONED_V1"
    );

    @Test void everyActiveProductionEventHasProducerEvidence() {
        var catalogueEvents = NotificationEventCatalogue.all().stream()
            .map(NotificationEventDefinition::eventType).collect(Collectors.toSet());
        assertThat(catalogueEvents).hasSize(14).contains("VEHICLE_GEOFENCE_TRANSITIONED_V1");
        assertThat(catalogueEvents).filteredOn(PRODUCTION_PRODUCERS::contains)
            .containsExactlyInAnyOrderElementsOf(PRODUCTION_PRODUCERS);
        assertThat(catalogueEvents).doesNotContain("DRIVER_DRUG_TEST_EXPIRING", "FUEL_LIMIT_EXCEEDED", "FUEL_EXCEPTION");
    }
}
