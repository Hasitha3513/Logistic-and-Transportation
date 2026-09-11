package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TrackingProviderResponses {
    private TrackingProviderResponses() { }

    public record ProviderType(
            String providerType, Set<String> capabilities, boolean supported) { }

    public record Connection(
            UUID id,
            String providerType,
            String displayName,
            String providerAlias,
            URI endpointUri,
            Map<String, String> safeConfiguration,
            boolean credentialConfigured,
            int pollIntervalSeconds,
            int pageSize,
            String lifecycle,
            String testStatus,
            Instant lastTestedAt,
            Instant lastSuccessfulPollAt,
            Instant lastProviderMessageAt,
            String lastErrorCategory,
            Instant nextPollAt,
            long version) { }

    public record ConnectionPage(
            List<Connection> items, int page, int size, long total) { }

    public record ConnectionTest(
            Connection connection, String status, String detailCode) { }

    public record DeviceBinding(
            UUID id,
            UUID trackingDeviceId,
            UUID providerConnectionId,
            String maskedExternalDeviceReference,
            Map<String, String> safeConfiguration,
            String lifecycle,
            Instant watermarkSourceTimestamp,
            Instant nextPollAt,
            long version) { }

    public record Discovery(
            String status, List<DiscoveredDevice> devices, String nextCursor) { }

    public record DiscoveredDevice(
            String maskedExternalDeviceReference,
            String displayName,
            Set<String> capabilities) { }
}
