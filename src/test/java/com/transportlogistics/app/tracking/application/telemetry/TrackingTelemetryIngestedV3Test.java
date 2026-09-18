package com.transportlogistics.app.tracking.application.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportlogistics.app.tracking.application.provider.ProviderCapability;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingTelemetryIngestedV3Test {
    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID VEHICLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DEVICE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final String IDENTITY = "a".repeat(64);
    private final JsonMapper json = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Test
    void roundTripsSeparatedEngineFactsWithoutLosingV2Signals() throws Exception {
        var event = event(TrackingTelemetryIngestedV3.IgnitionState.ON,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);

        var roundTrip = json.readValue(json.writeValueAsBytes(event),
                TrackingTelemetryIngestedV3.class);

        assertThat(roundTrip).isEqualTo(event);
        assertThat(roundTrip.eventType()).isEqualTo(TrackingTelemetryIngestedV1.TYPE);
        assertThat(roundTrip.eventVersion()).isEqualTo(3);
        assertThat(roundTrip.tamperState()).isEqualTo(TrackingTelemetryIngestedV2.TamperState.CLEAR);
        assertThat(roundTrip.engineRunningState())
                .isEqualTo(TrackingTelemetryIngestedV3.EngineRunningState.RUNNING);
    }

    @Test
    void absenceIsDistinctFromExplicitUnknownAndSourceIsRequiredForAnyReportedState() {
        assertThat(event(null, null, null).engineRunningState()).isNull();
        assertThat(event(EngineState.UNKNOWN, TrackingTelemetryIngestedV3.IgnitionState.UNKNOWN,
                TrackingTelemetryIngestedV3.EngineRunningState.UNKNOWN,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_STATUS)
                .engineRunningState()).isEqualTo(TrackingTelemetryIngestedV3.EngineRunningState.UNKNOWN);
        assertThatThrownBy(() -> event(null,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> event(null, null,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_RPM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void legacyEngineStateCanOnlyRetainIgnitionMeaning() {
        assertThatThrownBy(() -> event(EngineState.OFF,
                TrackingTelemetryIngestedV3.IgnitionState.ON, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ignition semantics");
    }

    @Test
    void identityIsStableAcrossCanonicalVersionsButDistinctObservationsRemainDistinct() {
        TrackingTelemetryIngestedV3 retry = event(null, null, null);
        TrackingTelemetryIngestedV3 distinct = event(UUID.randomUUID(), "b".repeat(64),
                null, null, null);

        assertThat(retry.eventId()).isEqualTo(EVENT_ID);
        assertThat(retry.dedupeIdentity()).isEqualTo(IDENTITY);
        assertThat(retry.eventType()).isEqualTo(TrackingTelemetryIngestedV2.TYPE);
        assertThat(distinct.eventId()).isNotEqualTo(retry.eventId());
        assertThat(distinct.dedupeIdentity()).isNotEqualTo(retry.dedupeIdentity());
    }

    @Test
    void historyVocabularyAcceptsEngineRunningWithoutActivatingProviderSupport() {
        assertThat(ProviderCapability.values()).extracting(Enum::name)
                .doesNotContain("ENGINE_RUNNING");
        assertThat(TelemetrySignalCapability.values()).extracting(Enum::name)
                .contains("ENGINE_RUNNING");
        assertThat(TrackingTelemetryIngestedV3.TOPIC)
                .isEqualTo("tracking.telemetry.ingested.v3");
        assertThat(TrackingTelemetryIngestedV3.DEAD_LETTER_TOPIC)
                .isEqualTo("tracking.telemetry.ingested.v3.dlt");
    }

    @Test
    void rejectsUnknownEnumValuesAndInvalidEnvelopeVersion() throws Exception {
        String serialized = json.writeValueAsString(event(null, null, null));
        assertThatThrownBy(() -> json.readValue(serialized.replace(
                "\"ignitionState\":null", "\"ignitionState\":\"STARTED\""),
                TrackingTelemetryIngestedV3.class)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new TrackingTelemetryIngestedV3(EVENT_ID,
                TrackingTelemetryIngestedV3.TYPE, 2, TENANT_ID, VEHICLE_ID, DEVICE_ID,
                "TEST_FIXTURE", "message-1", IDENTITY, new BigDecimal("6.9"),
                new BigDecimal("79.8"), BigDecimal.ZERO, null, new BigDecimal("4"), null,
                EngineState.UNKNOWN, null, null, Instant.parse("2026-09-18T10:00:00Z"),
                Instant.parse("2026-09-18T10:00:01Z"), null, null, null, null, null,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static TrackingTelemetryIngestedV3 event(
            TrackingTelemetryIngestedV3.IgnitionState ignition,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return event(EngineState.ON, ignition, running, source);
    }

    private static TrackingTelemetryIngestedV3 event(
            EngineState legacy,
            TrackingTelemetryIngestedV3.IgnitionState ignition,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return event(EVENT_ID, IDENTITY, legacy, ignition, running, source);
    }

    private static TrackingTelemetryIngestedV3 event(
            UUID eventId, String identity,
            TrackingTelemetryIngestedV3.IgnitionState ignition,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return event(eventId, identity, EngineState.ON, ignition, running, source);
    }

    private static TrackingTelemetryIngestedV3 event(
            UUID eventId, String identity, EngineState legacy,
            TrackingTelemetryIngestedV3.IgnitionState ignition,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return new TrackingTelemetryIngestedV3(eventId, TrackingTelemetryIngestedV3.TYPE,
                TrackingTelemetryIngestedV3.VERSION, TENANT_ID, VEHICLE_ID, DEVICE_ID,
                "TEST_FIXTURE", "message-1", identity, new BigDecimal("6.9"),
                new BigDecimal("79.8"), BigDecimal.ZERO, null, new BigDecimal("4"), null,
                legacy, null, null, Instant.parse("2026-09-18T10:00:00Z"),
                Instant.parse("2026-09-18T10:00:01Z"),
                TrackingTelemetryIngestedV2.TamperState.CLEAR, null, null, null, null,
                ignition, running, source);
    }
}
