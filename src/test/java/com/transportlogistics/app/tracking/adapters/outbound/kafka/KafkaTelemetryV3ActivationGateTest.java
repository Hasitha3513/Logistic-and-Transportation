package com.transportlogistics.app.tracking.adapters.outbound.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaTelemetryV3ActivationGateTest {
    @Test
    void routesV3OnlyToItsGovernedTopicAfterPersistenceIsReady() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        var publisher = new KafkaTelemetryStreamPublisher(kafka, new SimpleMeterRegistry(),
                "tracking.telemetry.ingested.v1", "tracking.telemetry.ingested.v2");
        TrackingTelemetryIngestedV3 event = event();

        assertThatThrownBy(() -> publisher.publishDurably(event.tenantId() + ":" + event.vehicleId(),
                event, "test", Duration.ofSeconds(1)))
                .isInstanceOf(com.transportlogistics.app.shared.domain.DependencyUnavailableException.class)
                .hasMessage("Telemetry stream is unavailable");
        verify(kafka).send(argThat((org.apache.kafka.clients.producer.ProducerRecord<String, Object> record)
                -> TrackingTelemetryIngestedV3.TOPIC.equals(record.topic())
                && event.equals(record.value())));
    }

    private static TrackingTelemetryIngestedV3 event() {
        return new TrackingTelemetryIngestedV3(UUID.randomUUID(),
                TrackingTelemetryIngestedV3.TYPE, TrackingTelemetryIngestedV3.VERSION,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "TEST_FIXTURE", null,
                "a".repeat(64), new BigDecimal("6.9"), new BigDecimal("79.8"), BigDecimal.ZERO,
                null, new BigDecimal("4"), null, EngineState.ON, null, null,
                Instant.parse("2026-09-18T10:00:00Z"), Instant.parse("2026-09-18T10:00:01Z"),
                null, null, null, null, null, TrackingTelemetryIngestedV3.IgnitionState.ON,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_RPM);
    }
}
