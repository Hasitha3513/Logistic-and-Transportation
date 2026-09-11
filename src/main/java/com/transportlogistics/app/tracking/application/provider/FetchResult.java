package com.transportlogistics.app.tracking.application.provider;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FetchResult(
        List<NormalizedPositionCandidate> candidates,
        Map<String, ProviderWatermark> nextWatermarks) {

    public FetchResult {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(nextWatermarks, "nextWatermarks");
        if (candidates.size() > 500 || candidates.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Fetch result exceeds 500 candidates");
        }
        if (nextWatermarks.size() > 500 || nextWatermarks.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null)) {
            throw new IllegalArgumentException("Fetch watermarks are invalid");
        }
        candidates = List.copyOf(candidates);
        nextWatermarks = Map.copyOf(nextWatermarks);
    }

    public static FetchResult empty() {
        return new FetchResult(List.of(), Map.of());
    }
}
