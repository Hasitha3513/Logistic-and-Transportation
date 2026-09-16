package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.BatteryChargingState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.ExternalPowerState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.TamperState;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class TraccarPayloadNormalizer extends AbstractJsonTelemetryPayloadNormalizer
        implements TelemetryPayloadNormalizer {
    private static final BigDecimal KNOTS_TO_KPH = new BigDecimal("1.852");

    public TraccarPayloadNormalizer(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(TelemetryGatewayType type) {
        return type == TelemetryGatewayType.TRACCAR;
    }

    @Override
    public NormalizedTelemetryPoint normalize(String rawPayload, UUID tenantId) {
        var root = json(rawPayload);
        var position = root.has("position") ? root.get("position") : root;
        var speedKnots = decimal(at(position, "/speed", false), false);
        JsonNode timestamp = position.hasNonNull("fixTime")
                ? position.get("fixTime") : position.get("deviceTime");
        var ignitionNode = at(position, "/attributes/ignition", false);
        var ignition = ignitionNode.isBoolean()
                ? ignitionNode.booleanValue() ? EngineState.ON : EngineState.OFF
                : EngineState.UNKNOWN;
        return new NormalizedTelemetryPoint(
                tenantId,
                text(at(root, "/device/uniqueId", true), true),
                text(at(position, "/id", false), false),
                instant(timestamp),
                decimal(at(position, "/latitude", true), true),
                decimal(at(position, "/longitude", true), true),
                speedKnots == null ? null : speedKnots.multiply(KNOTS_TO_KPH),
                decimal(at(position, "/course", false), false),
                decimal(at(position, "/accuracy", false), false),
                decimal(at(position, "/attributes/totalDistance", false), false),
                ignition,
                tamper(text(at(position, "/attributes/alarm", false), false)),
                decimal(at(position, "/attributes/batteryLevel", false), false),
                decimal(at(position, "/attributes/battery", false), false),
                power(at(position, "/attributes/power", false)),
                charging(at(position, "/attributes/charge", false)));
    }

    private static TamperState tamper(String alarm) {
        if (alarm == null) return null;
        return "tampering".equalsIgnoreCase(alarm) ? TamperState.DETECTED : TamperState.UNKNOWN;
    }

    private static ExternalPowerState power(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return null;
        if (value.isBoolean()) return value.booleanValue()
                ? ExternalPowerState.CONNECTED : ExternalPowerState.DISCONNECTED;
        return ExternalPowerState.UNKNOWN;
    }

    private static BatteryChargingState charging(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return null;
        if (value.isBoolean()) return value.booleanValue()
                ? BatteryChargingState.CHARGING : BatteryChargingState.NOT_CHARGING;
        return BatteryChargingState.UNKNOWN;
    }
}
