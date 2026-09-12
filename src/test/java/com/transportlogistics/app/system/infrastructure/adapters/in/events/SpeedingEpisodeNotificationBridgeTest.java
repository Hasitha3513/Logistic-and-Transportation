package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.VehicleSpeedingDetectedV1;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpeedingEpisodeNotificationBridgeTest {
    private final OperationalNotificationPublisher notifications = mock(OperationalNotificationPublisher.class);
    private final SpeedingEpisodeNotificationBridge bridge = new SpeedingEpisodeNotificationBridge(notifications);

    @Test
    void mapsWarningAndPreservesOnlyTheMinimizedMetadata() {
        UUID tenantId = UUID.randomUUID();
        VehicleSpeedingDetectedV1 event = event(tenantId, "WARNING");
        bridge.handle(event);

        var captured = forClass(OperationalNotificationEvent.class);
        verify(notifications).publish(captured.capture());
        assertThat(captured.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(captured.getValue().severity()).isEqualTo(OperationalNotificationEvent.Severity.WARNING);
        assertThat(captured.getValue().metadata()).doesNotContainKeys(
                "latitude", "longitude", "positionId", "deviceId", "providerId", "imei",
                "driverName", "driverPhone", "customerId", "credentials");
    }

    @Test
    void mapsHighWithoutCreatingADriverDisciplinaryMeaning() {
        bridge.handle(event(UUID.randomUUID(), "HIGH"));
        var captured = forClass(OperationalNotificationEvent.class);
        verify(notifications).publish(captured.capture());
        assertThat(captured.getValue().severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
        assertThat(captured.getValue().message()).isEqualTo(VehicleSpeedingDetectedV1.EVENT_TYPE);
    }

    @Test
    void permanentlyRejectsUnsupportedVersionAndUnexpectedPayload() {
        assertThatThrownBy(() -> bridge.handle(envelope(2, Map.of())))
                .isInstanceOf(PermanentEventFailureException.class);
        assertThatThrownBy(() -> bridge.handle(envelope(1, Map.of("latitude", 6.9))))
                .isInstanceOf(PermanentEventFailureException.class);
    }

    private static VehicleSpeedingDetectedV1 event(UUID tenantId, String severity) {
        UUID episode = UUID.randomUUID();
        return new VehicleSpeedingDetectedV1(episode, tenantId,
                OffsetDateTime.parse("2026-09-12T03:00:00Z"), UUID.randomUUID(), null, null, null,
                null, new BigDecimal("75"), new BigDecimal("60"), "TENANT_CONFIG",
                UUID.randomUUID(), 1, severity, "2026-09-12T03:00:00Z",
                "HIGH".equals(severity) ? 1 : 0);
    }

    private static DurableEventEnvelope envelope(int version, Map<String, ?> payload) {
        return new DurableEventEnvelope() {
            public UUID eventId() { return UUID.randomUUID(); }
            public String eventType() { return VehicleSpeedingDetectedV1.EVENT_TYPE; }
            public UUID tenantId() { return UUID.randomUUID(); }
            public OffsetDateTime occurredAt() { return OffsetDateTime.now(); }
            public int version() { return version; }
            public String aggregateType() { return "SPEEDING_EPISODE"; }
            public UUID aggregateId() { return eventId(); }
            public Map<String, ?> payload() { return payload; }
            public String durableConsumer() { return VehicleSpeedingDetectedV1.CONSUMER; }
        };
    }
}
