package com.transportlogistics.app.tracking.adapters.outbound.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.tracking.TrackingGpsExceptionOpenedV1;
import com.transportlogistics.app.tracking.TrackingGpsExceptionHighV1;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DurableGpsExceptionEventPublisherTest {
    private final List<DurableEventEnvelope> published = new ArrayList<>();
    private final DurableGpsExceptionEventPublisher publisher = new DurableGpsExceptionEventPublisher(published::add);

    @Test void warningPublishesOnlyMinimizedNotification() {
        publisher.publishOpened(episode(ExceptionType.PROCESSING_FAILURE, Severity.WARNING));
        assertThat(published).hasSize(1).first().isInstanceOf(TrackingGpsExceptionOpenedV1.class);
        assertThat(published.getFirst().payload().get("exceptionTypeLabel"))
                .isEqualTo("Telemetry processing failure");
        assertThat(published.getFirst().payload()).containsOnlyKeys(
                "exceptionTypeLabel", "severity", "vehicleLabel", "observedAt");
    }

    @Test void highPublishesNotificationAndOneOperationsFactWithAllowedMetadata() {
        publisher.publishOpened(episode(ExceptionType.PROCESSING_FAILURE, Severity.HIGH));
        assertThat(published).hasSize(2);
        TrackingGpsExceptionHighV1 fact = (TrackingGpsExceptionHighV1) published.get(1);
        assertThat(fact.category()).isEqualTo("TRACKING_DATA_QUALITY");
        assertThat(fact.safeMetadata()).containsOnlyKeys("episodeId", "exceptionType", "severity",
                "deviceId", "vehicleId", "openedAt", "lastObservedAt");
    }

    @Test void everyCanonicalTypeHasTheFrozenOperationsCategory() {
        Map<ExceptionType, String> expected = Map.ofEntries(
                Map.entry(ExceptionType.SIGNAL_LOSS, "TRACKING_CONNECTIVITY"),
                Map.entry(ExceptionType.BATTERY_LOW, "TRACKING_DEVICE_HEALTH"),
                Map.entry(ExceptionType.BATTERY_RAPID_DRAIN, "TRACKING_DEVICE_HEALTH"),
                Map.entry(ExceptionType.DEVICE_TAMPER, "TRACKING_DEVICE_SECURITY"),
                Map.entry(ExceptionType.BINDING_VIOLATION, "TRACKING_DEVICE_SECURITY"),
                Map.entry(ExceptionType.INVALID_TELEMETRY, "TRACKING_DATA_QUALITY"),
                Map.entry(ExceptionType.CLOCK_ANOMALY, "TRACKING_DATA_QUALITY"),
                Map.entry(ExceptionType.LOW_ACCURACY, "TRACKING_DATA_QUALITY"),
                Map.entry(ExceptionType.IMPOSSIBLE_MOVEMENT, "TRACKING_DATA_QUALITY"),
                Map.entry(ExceptionType.PROCESSING_FAILURE, "TRACKING_DATA_QUALITY"));
        expected.forEach((type, category) -> {
            published.clear(); publisher.publishHigh(episode(type, Severity.HIGH));
            assertThat(((TrackingGpsExceptionHighV1) published.getFirst()).category()).isEqualTo(category);
        });
    }

    private static GpsExceptionEpisode episode(ExceptionType type, Severity severity) {
        return GpsExceptionEpisode.open(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), type, severity, Instant.parse("2026-09-17T00:00:00Z"));
    }
}
