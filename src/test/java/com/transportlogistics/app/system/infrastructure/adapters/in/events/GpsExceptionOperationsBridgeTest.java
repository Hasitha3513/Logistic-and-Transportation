package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.tracking.TrackingGpsExceptionHighV1;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GpsExceptionOperationsBridgeTest {
    @Test void mapsTrackingHighFactWithoutAddingSensitiveMetadata() {
        List<DurableEventEnvelope> output = new ArrayList<>();
        var bridge = new GpsExceptionOperationsBridge(output::add);
        UUID episode = UUID.randomUUID();
        Map<String, String> metadata = Map.of("episodeId", episode.toString(),
                "exceptionType", "PROCESSING_FAILURE", "severity", "HIGH", "deviceId", UUID.randomUUID().toString(),
                "vehicleId", UUID.randomUUID().toString(), "openedAt", "2026-09-17T00:00:00Z",
                "lastObservedAt", "2026-09-17T00:00:00Z");
        bridge.handle(new TrackingGpsExceptionHighV1(UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-09-17T00:00:00Z"), episode, "PROCESSING_FAILURE",
                "TRACKING_DATA_QUALITY", metadata));
        OperationalExceptionFactV1 fact = (OperationalExceptionFactV1) output.getFirst();
        assertThat(fact.sourceModule()).isEqualTo(OperationalExceptionFactV1.SourceModule.TRACKING);
        assertThat(fact.categoryCandidate()).isEqualTo(OperationalExceptionFactV1.Category.TRACKING_DATA_QUALITY);
        assertThat(fact.safeMetadata()).isEqualTo(metadata);
    }
}
