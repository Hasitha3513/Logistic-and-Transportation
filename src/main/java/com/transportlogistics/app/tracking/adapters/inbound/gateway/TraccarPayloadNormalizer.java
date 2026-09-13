package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
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
                ignition);
    }
}
