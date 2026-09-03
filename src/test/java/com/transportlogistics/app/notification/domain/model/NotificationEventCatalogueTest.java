package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventCatalogueTest {
    @Test
    void exposesTheEightOperationalAndFiveDeliveryCustomerEvents() {
        assertThat(NotificationEventCatalogue.all()).extracting(NotificationEventDefinition::eventType)
            .containsExactlyInAnyOrder(
                "TRIP_DELAY_RECORDED", "TRIP_INCIDENT_RECORDED", "VEHICLE_MAINTENANCE_DUE",
                "VEHICLE_DOCUMENT_EXPIRING", "DRIVER_EXCEPTION_RECORDED", "DRIVER_MEDICAL_EXPIRING",
                "DRIVER_DRUG_TEST_FAILED", "DRIVER_LICENSE_EXPIRING", "DELIVERY_OUT_FOR_DELIVERY",
                "DELIVERY_ETA_RISK_CHANGED", "DELIVERY_COMPLETED", "DELIVERY_FAILED_ATTEMPT_RECORDED",
                "DELIVERY_REDELIVERY_SCHEDULED");
        assertThat(NotificationEventCatalogue.all()).hasSize(13);
    }

    @Test
    void excludesDeferredAndNotRequiredEvents() {
        assertThat(NotificationEventCatalogue.find("DRIVER_DRUG_TEST_EXPIRING")).isEmpty();
        assertThat(NotificationEventCatalogue.find("FUEL_LIMIT_EXCEEDED")).isEmpty();
        assertThat(NotificationEventCatalogue.find("FUEL_EXCEPTION")).isEmpty();
    }

    @Test
    void definesBothChannelsAndExactDelayVariables() {
        var delay = NotificationEventCatalogue.require("trip_delay_recorded");
        assertThat(delay.supportedChannels()).isEqualTo(Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));
        assertThat(delay.requiredVariables()).containsExactlyInAnyOrder(
            "eventTime", "severity", "tripId", "tripNumber", "delayMinutes", "reason");
        assertThat(delay.optionalVariables()).containsExactly("locationDescription");
        assertThat(delay.templateCode()).isEqualTo("TRIP_DELAY");
    }
}
