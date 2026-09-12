package com.transportlogistics.app.tracking.adapters.outbound.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DurableSpeedingEpisodePublisherTest {
    @Test
    void mapsTheExactPortPayloadToTheCanonicalDurableEnvelope() {
        DurableEventPublisher durable = mock(DurableEventPublisher.class);
        var adapter = new DurableSpeedingEpisodePublisher(durable);
        UUID episodeId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant source = Instant.parse("2026-09-12T03:00:00Z");

        adapter.publish(tenantId, new SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1(
                episodeId, UUID.randomUUID(), null, null, null, null,
                new SpeedKph(new BigDecimal("75.5")), new SpeedKph(new BigDecimal("60")),
                ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG, UUID.randomUUID(), 2,
                SpeedingEpisode.Severity.WARNING, source, 0));

        var captured = forClass(DurableEventEnvelope.class);
        verify(durable).publish(captured.capture());
        DurableEventEnvelope envelope = captured.getValue();
        assertThat(envelope.eventId()).isEqualTo(episodeId);
        assertThat(envelope.tenantId()).isEqualTo(tenantId);
        assertThat(envelope.occurredAt().toInstant()).isEqualTo(source);
        assertThat(envelope.payload().keySet()).containsExactlyInAnyOrder(
                "speedEpisodeId", "vehicleId", "driverId", "tripId", "routeId", "routeVersion",
                "observedSpeedKph", "effectiveThresholdKph", "thresholdSource", "ruleId",
                "ruleVersion", "severity", "sourceTimestamp", "repeatCount");
        assertThat(envelope.payload()).doesNotContainKeys(
                "latitude", "longitude", "positionId", "deviceId", "providerId", "imei");
    }
}
