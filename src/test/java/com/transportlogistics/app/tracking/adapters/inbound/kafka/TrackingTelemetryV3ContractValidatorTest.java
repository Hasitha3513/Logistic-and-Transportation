package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;

class TrackingTelemetryV3ContractValidatorTest {
    @Test
    void acceptsOnlyV3OnTheGovernedTopicWithTenantVehicleAuthority() {
        TrackingTelemetryIngestedV3 event = event();
        ConsumerRecord<String, TrackingTelemetryIngestedV3> record = record(event,
                TrackingTelemetryIngestedV3.TOPIC);

        assertThatCode(() -> TrackingTelemetryContractValidator.validate(record, event,
                TrackingTelemetryIngestedV3.TOPIC, TrackingTelemetryIngestedV3.VERSION))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TrackingTelemetryContractValidator.validate(record, event,
                "tracking.telemetry.ingested.v2", TrackingTelemetryIngestedV3.VERSION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrackingTelemetryContractValidator.validate(record, event,
                TrackingTelemetryIngestedV3.TOPIC, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ConsumerRecord<String, TrackingTelemetryIngestedV3> record(
            TrackingTelemetryIngestedV3 event, String topic) {
        var record = new ConsumerRecord<String, TrackingTelemetryIngestedV3>(topic, 0, 1,
                event.tenantId() + ":" + event.vehicleId(), event);
        record.headers().add(new RecordHeader("tenantId",
                event.tenantId().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventType",
                event.eventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("eventVersion",
                Integer.toString(event.eventVersion()).getBytes(StandardCharsets.UTF_8)));
        return record;
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
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);
    }
}
