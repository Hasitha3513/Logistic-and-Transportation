package com.transportlogistics.app.tracking.application.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class ProviderCapabilities {
    private static final ProviderCapabilities EMPTY = new ProviderCapabilities(Set.of());
    private final Set<ProviderCapability> values;

    private ProviderCapabilities(Set<ProviderCapability> values) {
        Objects.requireNonNull(values, "capabilities");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Capabilities cannot contain null");
        }
        this.values = values.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(values));
    }

    public static ProviderCapabilities empty() {
        return EMPTY;
    }

    public static ProviderCapabilities of(ProviderCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        if (capabilities.length == 0) {
            return EMPTY;
        }
        return new ProviderCapabilities(Set.copyOf(Arrays.asList(capabilities)));
    }

    public static ProviderCapabilities copyOf(Set<ProviderCapability> capabilities) {
        return capabilities.isEmpty() ? EMPTY : new ProviderCapabilities(capabilities);
    }

    public boolean has(ProviderCapability capability) {
        return values.contains(Objects.requireNonNull(capability, "capability"));
    }

    public boolean supportsAll(ProviderCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        return Arrays.stream(capabilities).allMatch(this::has);
    }

    public boolean supportsAny(ProviderCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        return Arrays.stream(capabilities).anyMatch(this::has);
    }

    public Set<ProviderCapability> asSet() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProviderCapabilities that && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
