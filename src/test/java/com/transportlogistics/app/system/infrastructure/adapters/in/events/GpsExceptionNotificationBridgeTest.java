package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.tracking.TrackingGpsExceptionOpenedV1;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GpsExceptionNotificationBridgeTest {
    @Test void highMapsToStoredCriticalWhileRetainingRenderedHighAndNoSensitiveData() {
        List<OperationalNotificationEvent> captured = new ArrayList<>();
        var bridge = new GpsExceptionNotificationBridge(captured::add);
        UUID id = UUID.randomUUID();
        bridge.handle(new TrackingGpsExceptionOpenedV1(id, UUID.randomUUID(), OffsetDateTime.parse("2026-09-17T00:00:00Z"),
                id, "Telemetry processing failure", "HIGH", "the assigned vehicle", "2026-09-17T00:00:00Z"));
        assertThat(captured).singleElement().satisfies(event -> {
            assertThat(event.severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
            assertThat(event.metadata()).containsEntry("severity", "HIGH").containsOnlyKeys(
                    "exceptionTypeLabel", "severity", "vehicleLabel", "observedAt");
            assertThat(event.metadata().toString()).doesNotContain("stack", "Exception", "redis", "kafka", "sql");
        });
    }
}
