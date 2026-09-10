package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderCapability;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderDiscoveryRequest;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnectionMutation;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TrackingProviderManagementService implements TrackingProviderManagementUseCase {
    private final TrackingProviderConnectionStore connections;
    private final TrackingDeviceProviderBindingStore bindings;
    private final TrackingProviderAdapterRegistry adapters;
    private final IntegrationSecretResolver secrets;
    private final Clock clock;

    public TrackingProviderManagementService(
            TrackingProviderConnectionStore connections,
            TrackingDeviceProviderBindingStore bindings,
            TrackingProviderAdapterRegistry adapters,
            IntegrationSecretResolver secrets,
            Clock clock) {
        this.connections = connections;
        this.bindings = bindings;
        this.adapters = adapters;
        this.secrets = secrets;
        this.clock = clock;
    }

    @Override
    public List<com.transportlogistics.app.tracking.application.provider.TrackingProviderDescriptor>
            providerTypes() {
        return adapters.descriptors();
    }

    @Override
    public TrackingProviderConnection create(Context context, CreateConnection command) {
        validate(command.providerType(), command.endpoint(), command.safeConfiguration());
        Instant now = clock.instant();
        return connections.create(new NewTrackingProviderConnection(
                required(context).tenantId(), context.actorId(), command.providerKeyId(),
                command.providerAlias(), command.credentialReference(), command.providerType(),
                command.displayName(), command.endpoint(), command.safeConfiguration(),
                command.pollIntervalSeconds(), command.pageSize(),
                ProviderConnectionLifecycle.DRAFT, now));
    }

    @Override
    public Page<TrackingProviderConnection> list(
            UUID tenantId, int page, int size,
            com.transportlogistics.app.tracking.application.provider.ProviderType providerType,
            ProviderConnectionLifecycle lifecycle,
            ProviderConnectionTestStatus testStatus) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.min(Math.max(size, 1), 100);
        List<TrackingProviderConnection> filtered = connections.list(
                Objects.requireNonNull(tenantId, "tenantId")).stream()
                .filter(value -> providerType == null || value.providerType().equals(providerType))
                .filter(value -> lifecycle == null || value.lifecycle() == lifecycle)
                .filter(value -> testStatus == null || value.testStatus() == testStatus)
                .toList();
        int from = Math.min(filtered.size(), boundedPage * boundedSize);
        int to = Math.min(filtered.size(), from + boundedSize);
        return new Page<>(filtered.subList(from, to), boundedPage, boundedSize, filtered.size());
    }

    @Override
    public TrackingProviderConnection get(UUID tenantId, ProviderConnectionId connectionId) {
        return connections.find(tenantId, connectionId.value())
                .orElseThrow(TrackingProviderManagementService::notFound);
    }

    @Override
    public TrackingProviderConnection update(
            Context context, ProviderConnectionId connectionId, UpdateConnection command) {
        TrackingProviderConnection current = get(required(context).tenantId(), connectionId);
        validate(current.providerType(), command.endpoint(), command.safeConfiguration());
        String credential = command.credentialReference() == null
                ? current.credentialReference() : command.credentialReference();
        return connections.update(context.tenantId(), connectionId.value(), command.version(),
                mutation(current, credential, command.displayName(), command.endpoint(),
                        command.safeConfiguration(), command.pollIntervalSeconds(), command.pageSize(),
                        current.lifecycle(), current.testStatus(), current.lastTestedAt(),
                        current.lastErrorCategory(), current.nextPollAt()),
                context.actorId(), clock.instant());
    }

    @Override
    public TestConnectionResult test(Context context, ProviderConnectionId connectionId) {
        TrackingProviderConnection current = get(required(context).tenantId(), connectionId);
        var adapter = adapters.require(current.providerType());
        char[] secret = secrets.resolve(current.credentialReference()).orElse(null);
        ConnectionTestResult result;
        if (secret == null) {
            result = new ConnectionTestResult(
                    ConnectionTestResult.Status.AUTH_FAILED, "credential_unavailable");
        } else {
            try {
                result = adapter.testConnection(execution(current), secret);
            } finally {
                Arrays.fill(secret, '\0');
            }
        }
        ProviderConnectionTestStatus status = ProviderConnectionTestStatus.valueOf(
                result.status().name());
        Instant now = clock.instant();
        TrackingProviderConnection updated = connections.update(
                context.tenantId(), connectionId.value(), current.version(),
                mutation(current, current.credentialReference(), current.displayName(),
                        current.endpoint(), current.safeConfiguration(), current.pollIntervalSeconds(),
                        current.pageSize(), current.lifecycle(), status, now,
                        result.detailCode() == null ? null : safeCategory(result.detailCode()),
                        current.nextPollAt()), context.actorId(), now);
        return new TestConnectionResult(updated, result);
    }

    @Override
    public TrackingProviderConnection lifecycle(
            Context context, ProviderConnectionId connectionId, long version,
            ProviderConnectionLifecycle lifecycle) {
        TrackingProviderConnection current = get(required(context).tenantId(), connectionId);
        if (lifecycle == ProviderConnectionLifecycle.DRAFT) {
            throw invalid("Provider connection cannot transition to DRAFT");
        }
        if (!validTransition(current.lifecycle(), lifecycle)) {
            throw invalid("Provider connection lifecycle transition is invalid");
        }
        if (lifecycle == ProviderConnectionLifecycle.ACTIVE) {
            validate(current.providerType(), current.endpoint(), current.safeConfiguration());
            char[] credential = requireCredential(current.credentialReference());
            Arrays.fill(credential, '\0');
        }
        Instant now = clock.instant();
        return connections.update(context.tenantId(), connectionId.value(), version,
                mutation(current, current.credentialReference(), current.displayName(),
                        current.endpoint(), current.safeConfiguration(), current.pollIntervalSeconds(),
                        current.pageSize(), lifecycle, current.testStatus(), current.lastTestedAt(),
                        current.lastErrorCategory(),
                        lifecycle == ProviderConnectionLifecycle.ACTIVE ? now : null),
                context.actorId(), now);
    }

    @Override
    public DiscoveryResult discover(
            Context context, ProviderConnectionId connectionId, int limit, String cursor) {
        TrackingProviderConnection current = get(required(context).tenantId(), connectionId);
        var adapter = adapters.require(current.providerType());
        if (!adapter.capabilities().has(ProviderCapability.DISCOVERY)) {
            throw new BusinessRuleException(
                    "TRACKING_PROVIDER_DISCOVERY_UNSUPPORTED",
                    "Provider device discovery is unsupported");
        }
        char[] secret = requireCredential(current.credentialReference());
        try {
            return adapter.discoverDevices(new ProviderDiscoveryRequest(
                    execution(current), Math.min(Math.max(limit, 1), 500), cursor), secret);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    @Override
    public TrackingDeviceProviderBinding bind(Context context, BindDevice command) {
        required(context);
        get(context.tenantId(), command.providerConnectionId());
        DeviceProviderBindingLifecycle lifecycle = Objects.requireNonNullElse(
                command.lifecycle(), DeviceProviderBindingLifecycle.DRAFT);
        return bindings.create(new NewTrackingDeviceProviderBinding(
                context.tenantId(), command.trackingDeviceId(), command.providerConnectionId(),
                command.externalDeviceReference(), command.safeConfiguration(), lifecycle,
                lifecycle == DeviceProviderBindingLifecycle.ACTIVE ? clock.instant() : null,
                context.actorId(), clock.instant()));
    }

    @Override
    public TrackingDeviceProviderBinding rebind(Context context, RebindDevice command) {
        required(context);
        get(context.tenantId(), command.providerConnectionId());
        Instant now = clock.instant();
        return bindings.rebind(new NewTrackingDeviceProviderBinding(
                context.tenantId(), command.trackingDeviceId(), command.providerConnectionId(),
                command.externalDeviceReference(), command.safeConfiguration(),
                DeviceProviderBindingLifecycle.ACTIVE, now, context.actorId(), now),
                command.currentBindingVersion());
    }

    @Override
    public java.util.Optional<TrackingDeviceProviderBinding> currentBinding(
            UUID tenantId, UUID trackingDeviceId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(trackingDeviceId, "trackingDeviceId");
        return bindings.findCurrentByDevice(tenantId, trackingDeviceId);
    }

    private void validate(
            com.transportlogistics.app.tracking.application.provider.ProviderType type,
            java.net.URI endpoint,
            com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration config) {
        var adapter = adapters.require(type);
        ConfigurationValidation result = adapter.validateConfiguration(
                new ProviderConnectionConfiguration(type, endpoint, config));
        if (result.status() != ConfigurationValidation.Status.VALID) {
            throw invalid("Provider connection configuration is invalid");
        }
    }

    private char[] requireCredential(String reference) {
        return secrets.resolve(reference).orElseThrow(() -> new BusinessRuleException(
                "TRACKING_PROVIDER_AUTH_FAILED", "Provider credential is unavailable"));
    }

    private static boolean validTransition(
            ProviderConnectionLifecycle current, ProviderConnectionLifecycle requested) {
        if (current == ProviderConnectionLifecycle.RETIRED) {
            return false;
        }
        return current == requested
                || requested == ProviderConnectionLifecycle.RETIRED
                || current == ProviderConnectionLifecycle.DRAFT
                        && requested == ProviderConnectionLifecycle.ACTIVE
                || current == ProviderConnectionLifecycle.ACTIVE
                        && requested == ProviderConnectionLifecycle.DISABLED
                || current == ProviderConnectionLifecycle.DISABLED
                        && requested == ProviderConnectionLifecycle.ACTIVE;
    }

    private static ProviderConnectionExecution execution(TrackingProviderConnection value) {
        return new ProviderConnectionExecution(value.id(), value.providerType(), value.providerAlias(),
                value.endpoint(), value.safeConfiguration());
    }

    private static TrackingProviderConnectionMutation mutation(
            TrackingProviderConnection current,
            String credentialReference,
            String displayName,
            java.net.URI endpoint,
            com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration configuration,
            int pollIntervalSeconds,
            int pageSize,
            ProviderConnectionLifecycle lifecycle,
            ProviderConnectionTestStatus testStatus,
            Instant lastTestedAt,
            String lastErrorCategory,
            Instant nextPollAt) {
        return new TrackingProviderConnectionMutation(
                credentialReference, current.providerType(), displayName, endpoint, configuration,
                pollIntervalSeconds, pageSize, lifecycle, testStatus, lastTestedAt,
                current.lastSuccessfulPollAt(), current.lastProviderMessageAt(), lastErrorCategory,
                nextPollAt, null, null);
    }

    private static String safeCategory(String value) {
        String safe = value.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        if (safe.isEmpty() || !Character.isLetter(safe.charAt(0))) {
            safe = "PROVIDER_" + safe;
        }
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }

    private static Context required(Context context) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(context.tenantId(), "tenantId");
        Objects.requireNonNull(context.actorId(), "actorId");
        return context;
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("TRACKING_PROVIDER_CONNECTION_INVALID", message);
    }

    private static NotFoundException notFound() {
        return new NotFoundException(
                "TRACKING_PROVIDER_CONNECTION_NOT_FOUND", "Tracking resource was not found");
    }
}
