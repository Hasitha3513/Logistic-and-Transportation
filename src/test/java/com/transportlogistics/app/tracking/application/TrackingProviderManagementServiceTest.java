package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.NewTrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.ProviderCapabilities;
import com.transportlogistics.app.tracking.application.provider.ProviderCapability;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionLifecycle;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionTestStatus;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderType;
import com.transportlogistics.app.tracking.application.provider.TrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnection;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderConnectionMutation;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TrackingProviderManagementServiceTest {
    private static final ProviderType FLESPI = ProviderType.of("FLESPI");
    private static final UUID TENANT = UUID.fromString("48000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.fromString("48000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
    private static final ProviderConnectionId CONNECTION_ID =
            new ProviderConnectionId(UUID.fromString("48000000-0000-0000-0000-000000000003"));
    private TrackingProviderConnectionStore connections;
    private TrackingDeviceProviderBindingStore bindings;
    private IntegrationSecretResolver secrets;
    private TestAdapter adapter;
    private TrackingProviderManagementService service;

    @BeforeEach
    void setUp() {
        connections = Mockito.mock(TrackingProviderConnectionStore.class);
        bindings = Mockito.mock(TrackingDeviceProviderBindingStore.class);
        secrets = Mockito.mock(IntegrationSecretResolver.class);
        adapter = new TestAdapter();
        service = new TrackingProviderManagementService(connections, bindings,
                new TrackingProviderAdapterRegistry(List.of(adapter)), secrets,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listsOnlyInstalledProviderDescriptors() {
        assertThat(service.providerTypes()).singleElement().satisfies(value -> {
            assertThat(value.providerType()).isEqualTo(FLESPI);
            assertThat(value.supported()).isTrue();
        });
    }

    @Test
    void createsValidatedDraftConnectionWithoutResolvingOrReturningSecret() {
        when(connections.create(any())).thenAnswer(invocation -> connection(
                invocation.getArgument(0, NewTrackingProviderConnection.class)));
        var created = service.create(context(), createCommand());
        assertThat(created.lifecycle()).isEqualTo(ProviderConnectionLifecycle.DRAFT);
        verify(secrets, never()).resolve(any());
        verify(connections).create(any(NewTrackingProviderConnection.class));
    }

    @Test
    void unsupportedOrProviderInvalidConfigurationNeverPersists() {
        var unsupported = createCommand(ProviderType.of("UNKNOWN"));
        assertThatThrownBy(() -> service.create(context(), unsupported))
                .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TRACKING_PROVIDER_TYPE_UNSUPPORTED"));
        adapter.validation = ConfigurationValidation.invalid(
                new ConfigurationValidation.ValidationIssue("endpoint", "invalid"));
        assertThatThrownBy(() -> service.create(context(), createCommand()))
                .isInstanceOf(BusinessRuleException.class);
        verify(connections, never()).create(any());
    }

    @Test
    void tenantScopedGetAndPaginationFiltersFailClosed() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(TENANT, CONNECTION_ID))
                .isInstanceOf(NotFoundException.class);
        when(connections.list(TENANT)).thenReturn(List.of(
                current(), otherConnection(ProviderConnectionLifecycle.DISABLED)));
        var page = service.list(TENANT, 0, 1, FLESPI,
                ProviderConnectionLifecycle.DRAFT, ProviderConnectionTestStatus.NOT_TESTED);
        assertThat(page.items()).containsExactly(current());
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void updatePreservesImmutableAuthorityAndOmittedCredential() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(current()));
        when(connections.update(eq(TENANT), eq(CONNECTION_ID.value()), eq(7L), any(), eq(ACTOR), eq(NOW)))
                .thenReturn(current());
        service.update(context(), CONNECTION_ID,
                new TrackingProviderManagementUseCase.UpdateConnection(
                        "Updated", URI.create("https://updated.example"),
                        ProviderSafeConfiguration.empty(), null, 10, 100, 7));
        ArgumentCaptor<TrackingProviderConnectionMutation> mutation =
                ArgumentCaptor.forClass(TrackingProviderConnectionMutation.class);
        verify(connections).update(eq(TENANT), eq(CONNECTION_ID.value()), eq(7L),
                mutation.capture(), eq(ACTOR), eq(NOW));
        assertThat(mutation.getValue().providerType()).isEqualTo(FLESPI);
        assertThat(mutation.getValue().credentialReference()).isEqualTo("env:FLESPI_ONE");
        assertThat(mutation.getValue().lifecycle()).isEqualTo(ProviderConnectionLifecycle.DRAFT);
    }

    @Test
    void testConnectionResolvesAndClearsSecretThenPersistsSafeStatus() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(current()));
        char[] token = "rotation-secret".toCharArray();
        when(secrets.resolve("env:FLESPI_ONE")).thenReturn(Optional.of(token));
        when(connections.update(eq(TENANT), eq(CONNECTION_ID.value()), anyLong(), any(), eq(ACTOR), eq(NOW)))
                .thenReturn(current());
        adapter.testResult = new ConnectionTestResult(
                ConnectionTestResult.Status.AUTH_FAILED, "provider_authentication");
        var result = service.test(context(), CONNECTION_ID);
        assertThat(result.result().status()).isEqualTo(ConnectionTestResult.Status.AUTH_FAILED);
        assertThat(token).containsOnly('\0');
        ArgumentCaptor<TrackingProviderConnectionMutation> mutation =
                ArgumentCaptor.forClass(TrackingProviderConnectionMutation.class);
        verify(connections).update(eq(TENANT), eq(CONNECTION_ID.value()), eq(7L),
                mutation.capture(), eq(ACTOR), eq(NOW));
        assertThat(mutation.getValue().testStatus())
                .isEqualTo(ProviderConnectionTestStatus.AUTH_FAILED);
        assertThat(mutation.getValue().lastErrorCategory()).isEqualTo("PROVIDER_AUTHENTICATION");
    }

    @Test
    void activationRequiresResolvableCredentialAndSchedulesImmediateCoordinatorEligibility() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(current()));
        assertThatThrownBy(() -> service.lifecycle(context(), CONNECTION_ID, 7,
                ProviderConnectionLifecycle.ACTIVE)).isInstanceOf(BusinessRuleException.class);
        char[] credential = "secret".toCharArray();
        when(secrets.resolve("env:FLESPI_ONE")).thenReturn(Optional.of(credential));
        when(connections.update(eq(TENANT), eq(CONNECTION_ID.value()), eq(7L), any(), eq(ACTOR), eq(NOW)))
                .thenReturn(current());
        service.lifecycle(context(), CONNECTION_ID, 7, ProviderConnectionLifecycle.ACTIVE);
        assertThat(credential).containsOnly('\0');
        ArgumentCaptor<TrackingProviderConnectionMutation> mutation =
                ArgumentCaptor.forClass(TrackingProviderConnectionMutation.class);
        verify(connections).update(eq(TENANT), eq(CONNECTION_ID.value()), eq(7L),
                mutation.capture(), eq(ACTOR), eq(NOW));
        assertThat(mutation.getValue().nextPollAt()).isEqualTo(NOW);
    }

    @Test
    void retiredProviderConnectionIsTerminal() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(
                otherConnection(ProviderConnectionLifecycle.RETIRED)));
        assertThatThrownBy(() -> service.lifecycle(context(), CONNECTION_ID, 7,
                ProviderConnectionLifecycle.ACTIVE))
                .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                        assertThat(exception.code()).isEqualTo(
                                "TRACKING_PROVIDER_CONNECTION_INVALID"));
        verify(connections, never()).update(any(), any(), anyLong(), any(), any(), any());
    }

    @Test
    void unsupportedDiscoveryFailsWithoutSecretResolutionOrTelemetry() {
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(current()));
        assertThatThrownBy(() -> service.discover(context(), CONNECTION_ID, 20, null))
                .isInstanceOfSatisfying(BusinessRuleException.class, exception ->
                        assertThat(exception.code()).isEqualTo(
                                "TRACKING_PROVIDER_DISCOVERY_UNSUPPORTED"));
        verify(secrets, never()).resolve(any());
    }

    @Test
    void bindAndRebindRemainTenantScopedAndDelegateToV76Authority() {
        UUID device = UUID.randomUUID();
        when(connections.find(TENANT, CONNECTION_ID.value())).thenReturn(Optional.of(current()));
        TrackingDeviceProviderBinding binding = binding(device);
        when(bindings.create(any())).thenReturn(binding);
        when(bindings.rebind(any(), eq(3L))).thenReturn(binding);
        assertThat(service.bind(context(), new TrackingProviderManagementUseCase.BindDevice(
                device, CONNECTION_ID, "external-1", ProviderSafeConfiguration.empty(),
                DeviceProviderBindingLifecycle.DRAFT))).isEqualTo(binding);
        assertThat(service.rebind(context(), new TrackingProviderManagementUseCase.RebindDevice(
                device, CONNECTION_ID, "external-2", ProviderSafeConfiguration.empty(), 3)))
                .isEqualTo(binding);
        ArgumentCaptor<NewTrackingDeviceProviderBinding> replacement =
                ArgumentCaptor.forClass(NewTrackingDeviceProviderBinding.class);
        verify(bindings).rebind(replacement.capture(), eq(3L));
        assertThat(replacement.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(replacement.getValue().lifecycle())
                .isEqualTo(DeviceProviderBindingLifecycle.ACTIVE);
    }

    private TrackingProviderManagementUseCase.Context context() {
        return new TrackingProviderManagementUseCase.Context(TENANT, ACTOR, "correlation");
    }

    private TrackingProviderManagementUseCase.CreateConnection createCommand() {
        return createCommand(FLESPI);
    }

    private TrackingProviderManagementUseCase.CreateConnection createCommand(ProviderType type) {
        return new TrackingProviderManagementUseCase.CreateConnection(
                type, "Flespi Account", "FLESPI_ONE", "provider-key-one",
                URI.create("https://flespi.example"), ProviderSafeConfiguration.empty(),
                "env:FLESPI_ONE", 5, 100);
    }

    private TrackingProviderConnection current() {
        return new TrackingProviderConnection(CONNECTION_ID, TENANT, "provider-key-one", "FLESPI_ONE",
                "env:FLESPI_ONE", FLESPI, "Flespi Account", URI.create("https://flespi.example"),
                ProviderSafeConfiguration.empty(), 5, 100, ProviderConnectionLifecycle.DRAFT,
                ProviderConnectionTestStatus.NOT_TESTED, null, null, null, null, null, null, null,
                NOW, ACTOR, NOW, ACTOR, 7);
    }

    private TrackingProviderConnection otherConnection(ProviderConnectionLifecycle lifecycle) {
        TrackingProviderConnection value = current();
        return new TrackingProviderConnection(new ProviderConnectionId(UUID.randomUUID()), TENANT,
                "provider-key-two", "FLESPI_TWO", "env:FLESPI_TWO", value.providerType(),
                "Other", value.endpoint(), value.safeConfiguration(), value.pollIntervalSeconds(),
                value.pageSize(), lifecycle, value.testStatus(), null, null, null, null, null, null,
                null, NOW, ACTOR, NOW, ACTOR, 0);
    }

    private TrackingProviderConnection connection(NewTrackingProviderConnection source) {
        return new TrackingProviderConnection(CONNECTION_ID, source.tenantId(), source.providerKeyId(),
                source.providerAlias(), source.credentialReference(), source.providerType(),
                source.displayName(), source.endpoint(), source.safeConfiguration(),
                source.pollIntervalSeconds(), source.pageSize(), source.lifecycle(),
                ProviderConnectionTestStatus.NOT_TESTED, null, null, null, null, null, null, null,
                source.now(), source.actorId(), source.now(), source.actorId(), 0);
    }

    private TrackingDeviceProviderBinding binding(UUID device) {
        return new TrackingDeviceProviderBinding(UUID.randomUUID(), TENANT, device, CONNECTION_ID,
                "external-1", ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                null, null, null, NOW, ACTOR, NOW, ACTOR, 0);
    }

    private static final class TestAdapter implements TrackingProviderAdapter {
        private ConfigurationValidation validation = ConfigurationValidation.valid();
        private ConnectionTestResult testResult = ConnectionTestResult.passed();
        private final AtomicReference<char[]> seenSecret = new AtomicReference<>();

        @Override public ProviderType providerType() { return FLESPI; }
        @Override public ProviderCapabilities capabilities() {
            return ProviderCapabilities.of(ProviderCapability.POLLING);
        }
        @Override public ConfigurationValidation validateConfiguration(
                com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration c) {
            return validation;
        }
        @Override public ConnectionTestResult testConnection(
                ProviderConnectionExecution connection, char[] secret) {
            seenSecret.set(secret);
            return testResult;
        }
        @Override public com.transportlogistics.app.tracking.application.provider.FetchResult fetchPositions(
                com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest request,
                char[] secret) {
            return com.transportlogistics.app.tracking.application.provider.FetchResult.empty();
        }
        @Override public ProviderHealth health(ProviderConnectionExecution connection) {
            return ProviderHealth.unknown();
        }
    }
}
