package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.BatteryChargingState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.ExternalPowerState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.TamperState;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class FlespiPayloadNormalizer extends AbstractJsonTelemetryPayloadNormalizer
        implements TelemetryPayloadNormalizer {

    public FlespiPayloadNormalizer(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(TelemetryGatewayType type) {
        return type == TelemetryGatewayType.FLESPI;
    }

    @Override
    public NormalizedTelemetryPoint normalize(String rawPayload, UUID tenantId) {
        var root = json(rawPayload);
        return new NormalizedTelemetryPoint(
                tenantId,
                text(at(root, "/ident", true), true),
                text(at(root, "/message/id", false), false),
                instant(at(root, "/timestamp", true)),
                decimal(at(root, "/position/latitude", true), true),
                decimal(at(root, "/position/longitude", true), true),
                decimal(at(root, "/position/speed", false), false),
                decimal(at(root, "/position/direction", false), false),
                decimal(at(root, "/position/accuracy", false), false),
                decimal(at(root, "/can/vehicle/mileage", false), false),
                EngineState.UNKNOWN,
                tamper(text(at(root, "/device/tampering/status", false), false)),
                decimal(at(root, "/battery/level", false), false),
                decimal(at(root, "/battery/voltage", false), false),
                externalPower(text(at(root, "/external/power/status", false), false)),
                charging(text(at(root, "/battery/charging/status", false), false)));
    }

    private static TamperState tamper(String value) {
        return value == null ? null : switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "detected", "true" -> TamperState.DETECTED;
            case "clear", "false" -> TamperState.CLEAR;
            default -> TamperState.UNKNOWN;
        };
    }

    private static ExternalPowerState externalPower(String value) {
        return value == null ? null : switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "connected", "true" -> ExternalPowerState.CONNECTED;
            case "disconnected", "false" -> ExternalPowerState.DISCONNECTED;
            default -> ExternalPowerState.UNKNOWN;
        };
    }

    private static BatteryChargingState charging(String value) {
        return value == null ? null : switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "charging", "true" -> BatteryChargingState.CHARGING;
            case "not_charging", "false" -> BatteryChargingState.NOT_CHARGING;
            default -> BatteryChargingState.UNKNOWN;
        };
    }
}
