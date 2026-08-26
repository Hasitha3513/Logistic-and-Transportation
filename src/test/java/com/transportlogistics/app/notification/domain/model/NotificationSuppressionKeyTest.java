package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationSuppressionKeyTest {
    @Test void isDeterministicAndNormalizesText() {
        UUID rule = UUID.randomUUID(); UUID aggregate = UUID.randomUUID();
        var first = NotificationSuppressionKey.of(rule, "TRIP_DELAY_RECORDED", "Trip", aggregate,
            "User@Example.test", NotificationChannel.EMAIL, null);
        var second = NotificationSuppressionKey.of(rule, " trip_delay_recorded ", "trip", aggregate,
            "user@example.test", NotificationChannel.EMAIL, " ");
        assertThat(first).isEqualTo(second);
        assertThat(first.value()).hasSize(64);
    }

    @Test void milestoneSeparatesSuppressionBuckets() {
        UUID rule = UUID.randomUUID(); UUID aggregate = UUID.randomUUID();
        assertThat(NotificationSuppressionKey.of(rule, "VEHICLE_DOCUMENT_EXPIRING", "Vehicle", aggregate,
            "ops", NotificationChannel.IN_APP, "30-days"))
            .isNotEqualTo(NotificationSuppressionKey.of(rule, "VEHICLE_DOCUMENT_EXPIRING", "Vehicle", aggregate,
                "ops", NotificationChannel.IN_APP, "7-days"));
    }
}
