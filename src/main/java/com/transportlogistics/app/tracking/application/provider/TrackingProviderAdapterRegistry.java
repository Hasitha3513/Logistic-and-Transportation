package com.transportlogistics.app.tracking.application.provider;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TrackingProviderAdapterRegistry {
    private final Map<ProviderType, TrackingProviderAdapter> adapters;
    private final List<TrackingProviderDescriptor> descriptors;

    public TrackingProviderAdapterRegistry(List<TrackingProviderAdapter> registeredAdapters) {
        Objects.requireNonNull(registeredAdapters, "registeredAdapters");
        Map<ProviderType, TrackingProviderAdapter> indexed = new LinkedHashMap<>();
        for (TrackingProviderAdapter adapter : registeredAdapters) {
            if (adapter == null) {
                throw new IllegalStateException("Tracking provider adapter cannot be null");
            }
            ProviderType providerType = adapter.providerType();
            if (providerType == null) {
                throw new IllegalStateException("Tracking provider adapter type cannot be null");
            }
            if (adapter.capabilities() == null) {
                throw new IllegalStateException("Tracking provider adapter capabilities cannot be null: "
                        + providerType);
            }
            TrackingProviderAdapter existing = indexed.putIfAbsent(providerType, adapter);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate Tracking provider adapter type: " + providerType);
            }
        }
        adapters = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
        List<TrackingProviderDescriptor> values = new ArrayList<>();
        adapters.forEach((type, adapter) -> values.add(
                new TrackingProviderDescriptor(type, adapter.capabilities(), true)));
        values.sort((left, right) -> left.providerType().compareTo(right.providerType()));
        descriptors = List.copyOf(values);
    }

    public java.util.Optional<TrackingProviderAdapter> find(ProviderType providerType) {
        return java.util.Optional.ofNullable(adapters.get(
                Objects.requireNonNull(providerType, "providerType")));
    }

    public TrackingProviderAdapter require(ProviderType providerType) {
        return find(providerType).orElseThrow(() -> new BusinessRuleException(
                "TRACKING_PROVIDER_TYPE_UNSUPPORTED", "Tracking provider type is unsupported"));
    }

    public List<TrackingProviderDescriptor> descriptors() {
        return descriptors;
    }
}
