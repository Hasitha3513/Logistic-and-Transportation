package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

public final class TrackingProviderRequests {
    private TrackingProviderRequests() { }

    public record CreateConnection(
            @NotBlank @Size(max = 64) String providerType,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(max = 80) String providerAlias,
            @NotBlank @Size(max = 160) String providerKeyId,
            URI endpointUri,
            @NotNull Map<String, String> safeConfiguration,
            @NotBlank @Size(max = 160) String credentialReference,
            @Min(5) @Max(86_400) int pollIntervalSeconds,
            @Min(1) @Max(500) int pageSize) { }

    public record UpdateConnection(
            @NotBlank @Size(max = 120) String displayName,
            URI endpointUri,
            @NotNull Map<String, String> safeConfiguration,
            @Size(max = 160) String credentialReference,
            @Min(5) @Max(86_400) int pollIntervalSeconds,
            @Min(1) @Max(500) int pageSize,
            @PositiveOrZero long version) { }

    public record Version(@PositiveOrZero long version) { }

    public record BindDevice(
            @NotNull UUID providerConnectionId,
            @NotBlank @Size(max = 160) String externalDeviceReference,
            @NotNull Map<String, String> safeConfiguration,
            @Size(max = 16) String lifecycle,
            @PositiveOrZero Long currentBindingVersion) { }

    public record RebindDevice(
            @NotNull UUID providerConnectionId,
            @NotBlank @Size(max = 160) String externalDeviceReference,
            @NotNull Map<String, String> safeConfiguration,
            @PositiveOrZero long currentBindingVersion) { }
}
