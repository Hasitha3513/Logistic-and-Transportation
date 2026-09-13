package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class GenericRestPayloadNormalizer extends AbstractJsonTelemetryPayloadNormalizer
        implements TelemetryPayloadNormalizer {

    public GenericRestPayloadNormalizer(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(TelemetryGatewayType type) {
        return type == TelemetryGatewayType.GENERIC;
    }

    @Override
    public NormalizedTelemetryPoint normalize(String rawPayload, UUID tenantId) {
        var root = json(rawPayload);
        var ignitionNode = at(root, "/ignitionOn", false);
        var ignition = ignitionNode.isBoolean()
                ? ignitionNode.booleanValue() ? EngineState.ON : EngineState.OFF
                : EngineState.UNKNOWN;
        return new NormalizedTelemetryPoint(
                tenantId,
                text(at(root, "/deviceId", true), true),
                text(at(root, "/messageId", false), false),
                instant(root.hasNonNull("recordedAt") ? root.get("recordedAt") : root.get("sourceTimestamp")),
                decimal(at(root, "/latitude", true), true),
                decimal(at(root, "/longitude", true), true),
                decimal(at(root, "/speedKmh", false), false),
                decimal(at(root, "/heading", false), false),
                decimal(root.has("accuracyMeters") ? root.get("accuracyMeters") : root.get("horizontalAccuracyMeters"), false),
                decimal(at(root, "/odometerKm", false), false),
                ignition);
    }
}
