package com.transportlogistics.app.tracking.adapters.inbound.gateway;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.application.provider.TelemetryGatewayType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class TelemetryPayloadNormalizerRegistry {
    private final Map<TelemetryGatewayType, TelemetryPayloadNormalizer> normalizers;

    public TelemetryPayloadNormalizerRegistry(List<TelemetryPayloadNormalizer> registered) {
        var indexed = new EnumMap<TelemetryGatewayType, TelemetryPayloadNormalizer>(
                TelemetryGatewayType.class);
        for (var type : TelemetryGatewayType.values()) {
            var matches = registered.stream().filter(normalizer -> normalizer.supports(type)).toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(
                        "Exactly one telemetry normalizer is required for " + type);
            }
            indexed.put(type, matches.getFirst());
        }
        normalizers = Map.copyOf(indexed);
    }

    public TelemetryPayloadNormalizer require(TelemetryGatewayType type) {
        var normalizer = normalizers.get(type);
        if (normalizer == null) {
            throw new BusinessRuleException(
                    "TRACKING_PROVIDER_TYPE_UNSUPPORTED", "Telemetry gateway type is unsupported");
        }
        return normalizer;
    }
}
