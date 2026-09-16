package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelemetryPayloadNormalizerTest {
    private static final UUID TENANT_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizesFlespiPositionWithoutTrustingPayloadTenant() {
        var point = new FlespiPayloadNormalizer(mapper).normalize("""
                {"tenantId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","ident":"FMC130-1",
                 "timestamp":1789290000,"position":{"latitude":6.927079,"longitude":79.861244,
                 "speed":42.5,"direction":187.25,"accuracy":4.2}}
                """, TENANT_ID);

        assertThat(point.tenantId()).isEqualTo(TENANT_ID);
        assertThat(point.externalDeviceReference()).isEqualTo("FMC130-1");
        assertThat(point.recordedAt()).isEqualTo(Instant.ofEpochSecond(1789290000));
        assertThat(point.speedKph()).isEqualByComparingTo("42.5");
        assertThat(point.headingDegrees()).isEqualByComparingTo("187.25");
    }

    @Test
    void convertsTraccarKnotsAndMapsIgnition() {
        var point = new TraccarPayloadNormalizer(mapper).normalize("""
                {"device":{"uniqueId":"TRACCAR-7"},"position":{"id":"91",
                 "fixTime":"2026-09-13T09:30:00Z","latitude":7.290572,"longitude":80.633728,
                 "speed":10,"course":90,"attributes":{"ignition":true}}}
                """, TENANT_ID);

        assertThat(point.speedKph()).isEqualByComparingTo(new BigDecimal("18.520"));
        assertThat(point.engineState()).isEqualTo(EngineState.ON);
        assertThat(point.providerMessageId()).isEqualTo("91");
    }

    @Test
    void normalizesCanonicalGenericPayload() {
        var normalizer = new GenericRestPayloadNormalizer(mapper);
        var point = normalizer.normalize("""
                {"tenantId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                 "vehicleId":"cccccccc-cccc-cccc-cccc-cccccccccccc",
                 "deviceId":"CUSTOM-3","messageId":"sample-3",
                 "sourceTimestamp":"2026-09-13T09:31:00Z",
                 "latitude":6.0329,"longitude":80.2168,"speedKmh":31.4,
                 "horizontalAccuracyMeters":4.5,"heading":15,
                 "ignitionOn":false,"odometerKm":9021.2}
                """, TENANT_ID);

        assertThat(normalizer.supports(TelemetryGatewayType.GENERIC)).isTrue();
        assertThat(point.tenantId()).isEqualTo(TENANT_ID);
        assertThat(point.providerMessageId()).isEqualTo("sample-3");
        assertThat(point.latitude()).isEqualByComparingTo("6.0329");
        assertThat(point.speedKph()).isEqualByComparingTo("31.4");
        assertThat(point.horizontalAccuracyMeters()).isEqualByComparingTo("4.5");
        assertThat(point.engineState()).isEqualTo(EngineState.OFF);
        assertThat(point.odometerKm()).isEqualByComparingTo("9021.2");
    }

    @Test
    void registryRequiresExactlyOneNormalizerForEverySupportedGateway() {
        var registry = new TelemetryPayloadNormalizerRegistry(List.of(
                new FlespiPayloadNormalizer(mapper),
                new TraccarPayloadNormalizer(mapper),
                new GenericRestPayloadNormalizer(mapper)));

        assertThat(registry.require(TelemetryGatewayType.FLESPI))
                .isInstanceOf(FlespiPayloadNormalizer.class);
        assertThat(registry.require(TelemetryGatewayType.TRACCAR))
                .isInstanceOf(TraccarPayloadNormalizer.class);
        assertThat(registry.require(TelemetryGatewayType.GENERIC))
                .isInstanceOf(GenericRestPayloadNormalizer.class);
    }

    @Test
    void mapsOnlyDocumentedFlespiSignalPaths() {
        var point = new FlespiPayloadNormalizer(mapper).normalize("""
                {"ident":"FMC130-FICTIONAL","timestamp":1789290000,
                 "position":{"latitude":6.9,"longitude":79.8},
                 "device":{"tampering":{"status":"detected"}},
                 "battery":{"level":0.000,"voltage":3.920000,"charging":{"status":"charging"}},
                 "external":{"power":{"status":"disconnected"}}}
                """, TENANT_ID);

        assertThat(point.tamperState().name()).isEqualTo("DETECTED");
        assertThat(point.batteryLevelPercent()).isEqualByComparingTo("0.000");
        assertThat(point.batteryVoltageVolts()).isEqualByComparingTo("3.920000");
        assertThat(point.externalPowerState().name()).isEqualTo("DISCONNECTED");
        assertThat(point.batteryChargingState().name()).isEqualTo("CHARGING");
    }

    @Test
    void mapsDocumentedTraccarAttributesAndRejectsGenericSignalPassthrough() {
        var traccar = new TraccarPayloadNormalizer(mapper).normalize("""
                {"device":{"uniqueId":"TRACCAR-FICTIONAL"},"position":{"id":"1",
                 "fixTime":"2026-09-16T10:00:00Z","latitude":6.9,"longitude":79.8,
                 "attributes":{"alarm":"tampering","batteryLevel":74.5,"battery":3.92,
                 "power":true,"charge":false}}}
                """, TENANT_ID);
        assertThat(traccar.tamperState().name()).isEqualTo("DETECTED");
        assertThat(traccar.batteryLevelPercent()).isEqualByComparingTo("74.5");
        assertThat(traccar.externalPowerState().name()).isEqualTo("CONNECTED");
        assertThat(traccar.batteryChargingState().name()).isEqualTo("NOT_CHARGING");

        var generic = new GenericRestPayloadNormalizer(mapper).normalize("""
                {"deviceId":"GENERIC-FICTIONAL","sourceTimestamp":"2026-09-16T10:00:00Z",
                 "latitude":6.9,"longitude":79.8,"tamperState":"DETECTED","batteryLevelPercent":80}
                """, TENANT_ID);
        assertThat(generic.tamperState()).isNull();
        assertThat(generic.batteryLevelPercent()).isNull();
    }
}
