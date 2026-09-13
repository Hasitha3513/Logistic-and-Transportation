package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
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
                EngineState.UNKNOWN);
    }
}
