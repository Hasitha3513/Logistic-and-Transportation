package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.transportlogistics.app.tracking.application.provider.NormalizedTelemetryPoint;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import java.util.UUID;

public interface TelemetryPayloadNormalizer {
    boolean supports(TelemetryGatewayType type);

    NormalizedTelemetryPoint normalize(String rawPayload, UUID tenantId);
}
