package com.transportlogistics.app.tracking.adapters.outbound.events;

import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.TrackingGpsExceptionOpenedV1;
import com.transportlogistics.app.tracking.TrackingGpsExceptionHighV1;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEventPublisherPort;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class DurableGpsExceptionEventPublisher implements GpsExceptionEventPublisherPort {
    private final DurableEventPublisher events;
    DurableGpsExceptionEventPublisher(DurableEventPublisher events) { this.events = events; }

    @Override public void publishOpened(GpsExceptionEpisode episode) {
        events.publish(new TrackingGpsExceptionOpenedV1(episode.id(), episode.tenantId(),
                utc(episode.openedAt()), episode.id(), label(episode.type()), episode.severity().name(),
                episode.vehicleId() == null ? "the assigned vehicle" : episode.vehicleId().toString(),
                episode.openedAt().toString()));
        if (episode.severity().name().equals("HIGH")) publishHigh(episode);
    }

    @Override public void publishHigh(GpsExceptionEpisode episode) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("episodeId", episode.id().toString());
        metadata.put("exceptionType", episode.type().name());
        metadata.put("severity", episode.severity().name());
        metadata.put("deviceId", episode.deviceId().toString());
        if (episode.vehicleId() != null) metadata.put("vehicleId", episode.vehicleId().toString());
        metadata.put("openedAt", episode.openedAt().toString());
        metadata.put("lastObservedAt", episode.lastObservedAt().toString());
        events.publish(new TrackingGpsExceptionHighV1(
                UUID.nameUUIDFromBytes((episode.id() + ":OPERATIONS:HIGH").getBytes(StandardCharsets.UTF_8)),
                episode.tenantId(), utc(episode.lastObservedAt()), episode.id(), episode.type().name(),
                category(episode.type()), Map.copyOf(metadata)));
    }

    private static OffsetDateTime utc(java.time.Instant value) { return OffsetDateTime.ofInstant(value, ZoneOffset.UTC); }
    private static String category(ExceptionType type) {
        return switch (type) {
            case SIGNAL_LOSS -> "TRACKING_CONNECTIVITY";
            case BATTERY_LOW, BATTERY_RAPID_DRAIN -> "TRACKING_DEVICE_HEALTH";
            case DEVICE_TAMPER, BINDING_VIOLATION -> "TRACKING_DEVICE_SECURITY";
            case INVALID_TELEMETRY, CLOCK_ANOMALY, LOW_ACCURACY, IMPOSSIBLE_MOVEMENT, PROCESSING_FAILURE ->
                    "TRACKING_DATA_QUALITY";
        };
    }
    private static String label(ExceptionType type) {
        return switch (type) {
            case PROCESSING_FAILURE -> "Telemetry processing failure";
            default -> java.util.Arrays.stream(type.name().toLowerCase(java.util.Locale.ROOT).split("_"))
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(java.util.stream.Collectors.joining(" "));
        };
    }
}
