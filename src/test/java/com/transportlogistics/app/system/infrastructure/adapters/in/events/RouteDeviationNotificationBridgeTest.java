package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.tracking.VehicleRouteDeviationDetectedV1;
import com.transportlogistics.app.tracking.VehicleRouteDeviationEscalatedV1;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteDeviationNotificationBridgeTest {
    private final CapturingPublisher notifications = new CapturingPublisher();

    @Test void warningMapsToWarningAndRoundsWithoutSensitiveContent() {
        var bridge = new RouteDeviationDetectedNotificationBridge(notifications);
        bridge.handle(detected("WARNING", new BigDecimal("149.5")));
        assertThat(notifications.event.severity()).isEqualTo(OperationalNotificationEvent.Severity.WARNING);
        assertThat(notifications.event.title()).endsWith("WARNING");
        assertThat(notifications.event.message()).contains("150 m").doesNotContain("Approval is required")
                .doesNotContainIgnoringCase("driver", "latitude", "provider", "credential");
    }

    @Test void domainHighMapsToCriticalWhileMessageRetainsHigh() {
        new RouteDeviationDetectedNotificationBridge(notifications)
                .handle(detected("HIGH", new BigDecimal("250.4")));
        assertThat(notifications.event.severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
        assertThat(notifications.event.title()).endsWith("HIGH");
        assertThat(notifications.event.message()).contains("250 m", "Approval is required.");
    }

    @Test void bothEscalationReasonsMapToCriticalAndRemainTruthful() {
        for (String reason : java.util.List.of("DISTANCE_HIGH", "REVIEW_REJECTED")) {
            CapturingPublisher publisher = new CapturingPublisher();
            new RouteDeviationEscalatedNotificationBridge(publisher).handle(escalated(reason));
            assertThat(publisher.event.severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
            assertThat(publisher.event.title()).endsWith(reason);
            assertThat(publisher.event.message()).contains("Reason: " + reason + ".");
        }
    }

    private static VehicleRouteDeviationDetectedV1 detected(String severity, BigDecimal distance) {
        UUID episode = UUID.randomUUID();
        return new VehicleRouteDeviationDetectedV1(episode, UUID.randomUUID(), OffsetDateTime.parse("2026-09-14T01:00:00Z"),
                episode, UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), "REVISION:1", severity,
                distance, new BigDecimal("100"), "2026-09-14T01:00:00Z", "HIGH".equals(severity));
    }
    private static VehicleRouteDeviationEscalatedV1 escalated(String reason) {
        return new VehicleRouteDeviationEscalatedV1(UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-09-14T01:00:00Z"), UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), UUID.randomUUID(), "REVISION:1", "HIGH", new BigDecimal("251"),
                new BigDecimal("100"), "2026-09-14T01:00:00Z", true, reason);
    }
    private static final class CapturingPublisher implements OperationalNotificationPublisher {
        private OperationalNotificationEvent event;
        @Override public void publish(OperationalNotificationEvent value) { event = value; }
    }
}
