package com.transportlogistics.app.tracking.application.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingTelemetryIngestedV2Test {
    private final JsonMapper json = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Test
    void optionalSignalsMayBeAbsentWithoutChangingV1PositionSemantics() throws Exception {
        var event = event(null, null, null, null, null);
        var roundTrip = json.readValue(json.writeValueAsBytes(event), TrackingTelemetryIngestedV2.class);

        assertThat(roundTrip).isEqualTo(event);
        assertThat(roundTrip.eventType()).isEqualTo(TrackingTelemetryIngestedV1.TYPE);
        assertThat(roundTrip.eventVersion()).isEqualTo(2);
        assertThat(roundTrip.tamperState()).isNull();
        assertThat(roundTrip.batteryLevelPercent()).isNull();
    }

    @Test
    void preservesZeroPrecisionAndEverySignalEnum() throws Exception {
        for (var tamper : TrackingTelemetryIngestedV2.TamperState.values()) {
            var event = event(tamper, new BigDecimal("0.000"), new BigDecimal("3.920000"),
                    TrackingTelemetryIngestedV2.ExternalPowerState.UNKNOWN,
                    TrackingTelemetryIngestedV2.BatteryChargingState.UNKNOWN);
            assertThat(json.readValue(json.writeValueAsBytes(event), TrackingTelemetryIngestedV2.class))
                    .isEqualTo(event);
        }
        assertThat(TrackingTelemetryIngestedV2.ExternalPowerState.values()).containsExactly(
                TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED,
                TrackingTelemetryIngestedV2.ExternalPowerState.DISCONNECTED,
                TrackingTelemetryIngestedV2.ExternalPowerState.UNKNOWN);
        assertThat(TrackingTelemetryIngestedV2.BatteryChargingState.values()).containsExactly(
                TrackingTelemetryIngestedV2.BatteryChargingState.CHARGING,
                TrackingTelemetryIngestedV2.BatteryChargingState.NOT_CHARGING,
                TrackingTelemetryIngestedV2.BatteryChargingState.UNKNOWN);
    }

    @Test
    void rejectsInvalidRangesScaleAndUnknownEnums() {
        assertThatThrownBy(() -> event(null, new BigDecimal("100.0001"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> event(null, null, new BigDecimal("1000.000001"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> event(null, new BigDecimal("-0.001"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> json.readValue(json(event(null, null, null, null, null))
                        .replace("\"tamperState\":null", "\"tamperState\":\"BROKEN\""),
                TrackingTelemetryIngestedV2.class)).isInstanceOf(Exception.class);
    }

    @Test
    void capabilityVocabularyIncludesConservativeUnknownState() {
        assertThat(TelemetryCapabilityState.values()).containsExactly(
                TelemetryCapabilityState.SUPPORTED,
                TelemetryCapabilityState.UNSUPPORTED,
                TelemetryCapabilityState.UNKNOWN);
        assertThat(TelemetrySignalCapability.values()).containsExactly(
                TelemetrySignalCapability.POSITION,
                TelemetrySignalCapability.SPEED,
                TelemetrySignalCapability.ACCURACY,
                TelemetrySignalCapability.HEADING,
                TelemetrySignalCapability.IGNITION,
                TelemetrySignalCapability.ENGINE_RUNNING,
                TelemetrySignalCapability.TAMPER,
                TelemetrySignalCapability.BATTERY_LEVEL,
                TelemetrySignalCapability.BATTERY_VOLTAGE,
                TelemetrySignalCapability.EXTERNAL_POWER,
                TelemetrySignalCapability.BATTERY_CHARGING);
    }

    private String json(TrackingTelemetryIngestedV2 event) throws Exception {
        return json.writeValueAsString(event);
    }

    private static TrackingTelemetryIngestedV2 event(
            TrackingTelemetryIngestedV2.TamperState tamper, BigDecimal level, BigDecimal voltage,
            TrackingTelemetryIngestedV2.ExternalPowerState power,
            TrackingTelemetryIngestedV2.BatteryChargingState charging) {
        return new TrackingTelemetryIngestedV2(UUID.fromString("10000000-0000-0000-0000-000000000001"),
                TrackingTelemetryIngestedV2.TYPE, 2,
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                UUID.fromString("40000000-0000-0000-0000-000000000004"), "FLESPI", "fictional-1",
                "a".repeat(64), new BigDecimal("6.9000"), new BigDecimal("79.8000"),
                BigDecimal.ZERO, null, null, null, EngineState.UNKNOWN, null, null,
                Instant.parse("2026-09-16T10:00:00Z"), Instant.parse("2026-09-16T10:00:01Z"),
                tamper, level, voltage, power, charging);
    }
}
