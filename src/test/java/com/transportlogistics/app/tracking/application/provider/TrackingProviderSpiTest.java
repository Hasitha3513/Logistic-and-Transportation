package com.transportlogistics.app.tracking.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.ValidationIssue;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingProviderSpiTest {
    private static final ProviderType FLESPI = ProviderType.of("FLESPI");
    private static final ProviderType TEST_PROVIDER = ProviderType.of("TEST_PROVIDER");
    private static final char[] TEST_SECRET = "test-only-sensitive-value".toCharArray();

    @Test
    void providerTypeAcceptsStrictValidIdentifiersAndBoundaryLength() {
        assertThat(List.of("FLESPI", "TRACCAR", "WIALON", "GEOTAB", "SAMSARA",
                "TELTONIKA_HTTP", "A" + "1".repeat(63)))
                .allSatisfy(value -> assertThat(ProviderType.of(value).value()).isEqualTo(value));
        assertThat(ProviderType.of("FLESPI").toString()).isEqualTo("FLESPI");
        assertThat(ProviderType.of("FLESPI")).isEqualTo(ProviderType.of("FLESPI"));
    }

    @Test
    void providerTypeRejectsNullEmptyWhitespaceLowercaseAndInvalidCharacters() {
        for (String invalid : new String[] {"", " ", "flespi", " FLESPI ", "1FLESPI",
                "FLESPI-REST", "A".repeat(65)}) {
            assertThatThrownBy(() -> ProviderType.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> ProviderType.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void capabilitiesAreImmutableValueBasedAndDeduplicateInput() {
        ProviderCapabilities capabilities = ProviderCapabilities.of(
                ProviderCapability.POLLING, ProviderCapability.SPEED,
                ProviderCapability.POLLING);
        assertThat(capabilities.has(ProviderCapability.POLLING)).isTrue();
        assertThat(capabilities.supportsAll(
                ProviderCapability.POLLING, ProviderCapability.SPEED)).isTrue();
        assertThat(capabilities.supportsAny(
                ProviderCapability.MQTT, ProviderCapability.SPEED)).isTrue();
        assertThat(capabilities.asSet()).containsExactlyInAnyOrder(
                ProviderCapability.POLLING, ProviderCapability.SPEED);
        assertThatThrownBy(() -> capabilities.asSet().add(ProviderCapability.MQTT))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(capabilities).isEqualTo(ProviderCapabilities.copyOf(
                Set.of(ProviderCapability.SPEED, ProviderCapability.POLLING)));
    }

    @Test
    void capabilitiesSupportEmptySetAndRejectNulls() {
        assertThat(ProviderCapabilities.empty().asSet()).isEmpty();
        assertThat(ProviderCapabilities.empty().supportsAll()).isTrue();
        assertThat(ProviderCapabilities.empty().supportsAny()).isFalse();
        assertThatThrownBy(() -> ProviderCapabilities.of((ProviderCapability[]) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ProviderCapabilities.of(ProviderCapability.POLLING, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void safeConfigurationIsBoundedImmutableAndRejectsSecretLikeKeys() {
        Map<String, String> source = new HashMap<>();
        source.put("channel.id", "safe-channel");
        ProviderSafeConfiguration configuration = new ProviderSafeConfiguration(source);
        source.put("poll.mode", "later-mutation");
        assertThat(configuration.values()).containsOnlyKeys("channel.id");
        assertThatThrownBy(() -> configuration.values().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
        for (String key : List.of("token", "api_key", "clientSecret", "password")) {
            assertThatThrownBy(() -> new ProviderSafeConfiguration(Map.of(key, "value")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsafe configuration key");
        }
    }

    @Test
    void providerNeutralRequestsAndResultsDefensivelyCopyCollections() {
        ProviderConnectionExecution connection = connection(FLESPI);
        var devices = new java.util.ArrayList<>(List.of(
                new ProviderDeviceCursor("masked-device-1", new ProviderWatermark(null, null))));
        ProviderFetchRequest request = new ProviderFetchRequest(
                connection, devices, 500, 1_048_576, Instant.now().plusSeconds(10));
        devices.clear();
        assertThat(request.devices()).hasSize(1);

        var candidates = new java.util.ArrayList<>(List.of(candidate()));
        Map<String, ProviderWatermark> watermarks = new HashMap<>();
        watermarks.put("masked-device-1", new ProviderWatermark(Instant.now(), null));
        FetchResult result = new FetchResult(candidates, watermarks);
        candidates.clear();
        watermarks.clear();
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.nextWatermarks()).hasSize(1);
    }

    @Test
    void providerFetchRequestEnforcesHardBounds() {
        ProviderConnectionExecution connection = connection(FLESPI);
        List<ProviderDeviceCursor> devices = List.of(
                new ProviderDeviceCursor("device", null));
        assertThatThrownBy(() -> new ProviderFetchRequest(
                connection, devices, 501, 100, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProviderFetchRequest(
                connection, devices, 1, 1_048_577, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizedCandidateIsProviderNeutralAndHonestAboutOptionalFacts() {
        NormalizedPositionCandidate candidate = candidate();
        assertThat(candidate.externalDeviceReference()).isEqualTo("masked-device-1");
        assertThat(candidate.engineState()).isEqualTo(EngineState.UNKNOWN);
        assertThat(candidate.horizontalAccuracyMeters()).isNull();
        assertThat(candidate.providerMessageId()).isNull();
        assertThat(candidate.providerSequence()).isNull();
    }

    @Test
    void validationAndHealthResultsAreNullSafeAndBounded() {
        assertThat(ConfigurationValidation.valid().status())
                .isEqualTo(ConfigurationValidation.Status.VALID);
        ConfigurationValidation invalid = ConfigurationValidation.invalid(
                new ValidationIssue("ENDPOINT_REQUIRED", "An endpoint is required"));
        assertThat(invalid.status()).isEqualTo(ConfigurationValidation.Status.INVALID);
        assertThat(ProviderHealth.unknown().state()).isEqualTo(ProviderHealth.State.UNKNOWN);
        assertThat(ConnectionTestResult.passed().status())
                .isEqualTo(ConnectionTestResult.Status.PASS);
        assertThatThrownBy(() -> new ProviderHealth(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaultDiscoveryIsExplicitlyUnsupported() {
        TrackingProviderAdapter adapter = adapter(FLESPI, ProviderCapability.POLLING);
        DiscoveryResult result = adapter.discoverDevices(
                new ProviderDiscoveryRequest(connection(FLESPI), 100, null), TEST_SECRET);
        assertThat(result.status()).isEqualTo(DiscoveryResult.Status.UNSUPPORTED);
        assertThat(result.devices()).isEmpty();
    }

    @Test
    void registryDiscoversDummySecondAdapterWithoutSwitchOrDomainChange() {
        TrackingProviderAdapter flespi = adapter(FLESPI, ProviderCapability.POLLING);
        TrackingProviderAdapter test = adapter(TEST_PROVIDER, ProviderCapability.WEBHOOK);
        TrackingProviderAdapterRegistry registry =
                new TrackingProviderAdapterRegistry(List.of(test, flespi));
        assertThat(registry.require(FLESPI)).isSameAs(flespi);
        assertThat(registry.require(TEST_PROVIDER)).isSameAs(test);
        assertThat(registry.descriptors()).extracting(
                descriptor -> descriptor.providerType().value())
                .containsExactly("FLESPI", "TEST_PROVIDER");
    }

    @Test
    void registryRejectsDuplicateNullAdapterTypeAndCapabilitiesDeterministically() {
        assertThatThrownBy(() -> new TrackingProviderAdapterRegistry(
                List.of(adapter(FLESPI), adapter(FLESPI))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Duplicate Tracking provider adapter type: FLESPI");
        assertThatThrownBy(() -> new TrackingProviderAdapterRegistry(
                java.util.Arrays.asList((TrackingProviderAdapter) null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tracking provider adapter cannot be null");
        assertThatThrownBy(() -> new TrackingProviderAdapterRegistry(
                List.of(adapter(null))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tracking provider adapter type cannot be null");
    }

    @Test
    void registryUnsupportedTypeUsesStableBusinessError() {
        TrackingProviderAdapterRegistry registry = new TrackingProviderAdapterRegistry(List.of());
        assertThat(registry.find(FLESPI)).isEmpty();
        assertThatThrownBy(() -> registry.require(FLESPI))
                .isInstanceOfSatisfying(BusinessRuleException.class, failure -> {
                    assertThat(failure.code()).isEqualTo("TRACKING_PROVIDER_TYPE_UNSUPPORTED");
                    assertThat(failure.getMessage()).isEqualTo(
                            "Tracking provider type is unsupported");
                });
    }

    @Test
    void secretDoesNotAppearInProviderNeutralValuesOrRegistryDiagnostics() {
        String secret = new String(TEST_SECRET);
        TrackingProviderAdapterRegistry registry = new TrackingProviderAdapterRegistry(
                List.of(adapter(FLESPI)));
        assertThat(connection(FLESPI).toString()).doesNotContain(secret);
        assertThat(registry.descriptors().toString()).doesNotContain(secret);
        assertThatThrownBy(() -> registry.require(TEST_PROVIDER))
                .hasMessageNotContaining(secret);
    }

    private static ProviderConnectionExecution connection(ProviderType providerType) {
        return new ProviderConnectionExecution(
                new ProviderConnectionId(UUID.randomUUID()), providerType, providerType.value(),
                URI.create("https://provider.example"),
                new ProviderSafeConfiguration(Map.of("channel.id", "safe-channel")));
    }

    private static NormalizedPositionCandidate candidate() {
        return new NormalizedPositionCandidate(
                "masked-device-1", Instant.parse("2026-09-09T00:00:00Z"),
                new BigDecimal("6.927079"), new BigDecimal("79.861244"),
                null, null, null, null, null, null, null, null, null);
    }

    private static TrackingProviderAdapter adapter(
            ProviderType type, ProviderCapability... capabilities) {
        return new TrackingProviderAdapter() {
            @Override
            public ProviderType providerType() {
                return type;
            }

            @Override
            public ProviderCapabilities capabilities() {
                return ProviderCapabilities.of(capabilities);
            }

            @Override
            public ConfigurationValidation validateConfiguration(
                    ProviderConnectionConfiguration configuration) {
                return ConfigurationValidation.valid();
            }

            @Override
            public ConnectionTestResult testConnection(
                    ProviderConnectionExecution connection, char[] secret) {
                return ConnectionTestResult.passed();
            }

            @Override
            public FetchResult fetchPositions(ProviderFetchRequest request, char[] secret) {
                return FetchResult.empty();
            }

            @Override
            public ProviderHealth health(ProviderConnectionExecution connection) {
                return ProviderHealth.unknown();
            }
        };
    }
}
