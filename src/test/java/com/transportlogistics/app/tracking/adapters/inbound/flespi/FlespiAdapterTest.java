package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBinding;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlespiAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final char[] SECRET = "controlled-secret-value".toCharArray();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private FlespiAdapterProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FlespiAdapterProperties();
        properties.setEnabled(true);
        properties.setProviderKeyId("flespi-key-1");
        properties.setProviderAlias("FLESPI");
        properties.setFlespiDeviceId(42);
        properties.setDeviceIdent("masked-fmc130-ident");
        properties.setTrackingDeviceId(DEVICE);
    }

    @Test
    void configurationBoundsPollPageAndOverlapAndRequiresTlsProvider() {
        properties.setPollInterval(Duration.ofSeconds(1));
        properties.setPageSize(900);
        properties.setOverlapWindow(Duration.ofMinutes(20));
        assertThat(properties.getPollInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getPageSize()).isEqualTo(500);
        assertThat(properties.getOverlapWindow()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.configured()).isTrue();
        properties.setBaseUrl(URI.create("http://flespi.example"));
        assertThat(properties.configured()).isFalse();
    }

    @Test
    void providerRequestIsTokenAuthenticatedDeviceScopedAndBounded() {
        AtomicReference<URI> uri = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        var client = new FlespiProviderClient(properties, json, (requestUri, authorization, timeout) -> {
            uri.set(requestUri);
            auth.set(authorization);
            return new FlespiProviderClient.HttpResult(200, "{\"result\":[]}".getBytes(StandardCharsets.UTF_8));
        });
        client.fetch(SECRET, NOW.minusSeconds(300), NOW);
        assertThat(uri.get().getScheme()).isEqualTo("https");
        assertThat(uri.get().getPath()).isEqualTo("/gw/devices/42/messages");
        assertThat(uri.get().getQuery()).contains("count=500", "from=", "to=");
        assertThat(auth.get()).isEqualTo("FlespiToken controlled-secret-value");
    }

    @Test
    void providerRejectsAuthenticationTransientMalformedAndOversizeResponses() {
        assertFailure(401, "{}", FlespiFailure.Kind.AUTHENTICATION);
        assertFailure(429, "{}", FlespiFailure.Kind.TRANSIENT);
        assertFailure(503, "{}", FlespiFailure.Kind.TRANSIENT);
        assertFailure(200, "{}", FlespiFailure.Kind.PERMANENT);
        String messages = "{\"result\":[" + "{},".repeat(500) + "{}]}";
        assertFailure(200, messages, FlespiFailure.Kind.PERMANENT);
    }

    @Test
    void networkFailureIsTransientAndSanitized() {
        var client = new FlespiProviderClient(properties, json, (uri, authorization, timeout) -> {
            throw new java.io.IOException("controlled provider outage");
        });
        assertThatThrownBy(() -> client.fetch(SECRET, NOW.minusSeconds(1), NOW))
                .isInstanceOfSatisfying(FlespiFailure.class, failure -> {
                    assertThat(failure.kind()).isEqualTo(FlespiFailure.Kind.TRANSIENT);
                    assertThat(failure.getMessage()).doesNotContain(new String(SECRET), "masked-fmc130-ident");
                });
    }

    @Test
    void mapsDocumentationAlignedFieldsAndIgnoresPayloadTenantAuthority() throws Exception {
        JsonNode source = fixture().path("result").get(0);
        var mapped = new FlespiMessageMapper(properties).map(source);
        assertThat(mapped.deviceId()).isEqualTo(DEVICE);
        assertThat(mapped.sourceTimestamp()).isEqualTo(Instant.parse("2026-09-09T00:00:00.125Z"));
        assertThat(mapped.latitude()).isEqualByComparingTo("6.927079");
        assertThat(mapped.longitude()).isEqualByComparingTo("79.861244");
        assertThat(mapped.horizontalAccuracyMeters()).isEqualByComparingTo("3.5");
        assertThat(mapped.speedKph()).isEqualByComparingTo("42.25");
        assertThat(mapped.headingDegrees()).isEqualByComparingTo("187.5");
        assertThat(mapped.safeMetadata()).containsExactly(
                org.assertj.core.data.MapEntry.entry("source", "flespi-rest"));
    }

    @Test
    void rejectsEachMessageMissingMandatoryIdentityTimeOrCoordinates() throws Exception {
        var mapper = new FlespiMessageMapper(properties);
        JsonNode valid = fixture().path("result").get(0);
        for (String field : List.of("ident", "timestamp", "position.latitude", "position.longitude")) {
            JsonNode copy = valid.deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) copy).remove(field);
            assertThatThrownBy(() -> mapper.map(copy)).isInstanceOf(FlespiFailure.class);
        }
        ((com.fasterxml.jackson.databind.node.ObjectNode) valid).put("ident", "different-device");
        assertThatThrownBy(() -> mapper.map(valid)).isInstanceOf(FlespiFailure.class);
    }

    @Test
    void missingOptionalFieldsRemainAbsentWithoutSyntheticObservations() throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) fixture().path("result").get(0).deepCopy();
        node.remove(List.of("position.accuracy", "position.speed", "position.direction"));
        var mapped = new FlespiMessageMapper(properties).map(node);
        assertThat(mapped.horizontalAccuracyMeters()).isNull();
        assertThat(mapped.speedKph()).isNull();
        assertThat(mapped.headingDegrees()).isNull();
    }

    @Test
    void bridgeSignsExactRawBodyAndUsesFreshNonce() throws Exception {
        AtomicReference<TrackingIngressBridge.SignedIngressRequest> first = new AtomicReference<>();
        AtomicReference<TrackingIngressBridge.SignedIngressRequest> second = new AtomicReference<>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        TrackingIngressBridge.IngressTransport transport = request -> {
            if (calls.getAndIncrement() == 0) first.set(request); else second.set(request);
            return 200;
        };
        var bridge = new TrackingIngressBridge(properties, json, Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry(), transport, new SecureRandom());
        var position = mappedPosition();
        bridge.ingest(List.of(position), SECRET);
        bridge.ingest(List.of(position), SECRET);
        String canonical = first.get().epoch() + "\n" + first.get().nonce() + "\n"
                + properties.getProviderKeyId() + "\n" + properties.getProviderAlias() + "\n" + first.get().body();
        assertThat(first.get().signature()).isEqualTo(hmac(new String(SECRET), canonical));
        assertThat(first.get().nonce()).hasSize(64).isNotEqualTo(second.get().nonce());
        assertThat(first.get().body()).doesNotContain("providerMessageId", "engineHours", "tenant");
    }

    @Test
    void bridgeRejectsDownstreamAndOversizeBatch() {
        var bridge = new TrackingIngressBridge(properties, json, Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry(), request -> 503, new SecureRandom());
        assertThatThrownBy(() -> bridge.ingest(List.of(mappedPosition()), SECRET))
                .isInstanceOf(FlespiFailure.class);
        assertThatThrownBy(() -> bridge.ingest(java.util.Collections.nCopies(501, mappedPosition()), SECRET))
                .isInstanceOf(FlespiFailure.class);
    }

    @Test
    void pollingUsesBindingSecretColdOverlapAndAdvancesWatermarkOnlyAfterIngress() throws Exception {
        var harness = harness(List.of(fixture().path("result").get(0)), 200);
        harness.adapter().pollNow();
        assertThat(harness.adapter().watermark()).isEqualTo(Instant.parse("2026-09-09T00:00:00.125Z"));
        assertThat(harness.bindingLookups().get()).isEqualTo(1);
        assertThat(harness.secretLookups().get()).isEqualTo(1);
        assertThat(harness.state().snapshot().reachable()).isTrue();
    }

    @Test
    void downstreamFailureDoesNotAdvanceWatermarkAndSchedulesRetry() throws Exception {
        var harness = harness(List.of(fixture().path("result").get(0)), 503);
        harness.adapter().pollNow();
        assertThat(harness.adapter().watermark()).isNull();
        assertThat(harness.adapter().retryAttempt()).isEqualTo(1);
        assertThat(harness.adapter().nextAttempt()).isAfterOrEqualTo(NOW.plusSeconds(5));
        assertThat(harness.state().snapshot().lastFailureCategory())
                .isEqualTo(FlespiAdapterState.FailureCategory.DOWNSTREAM);
    }

    @Test
    void disabledOrIncompleteConfigurationNeverCallsProvider() {
        properties.setEnabled(false);
        AtomicInteger providerCalls = new AtomicInteger();
        FlespiProviderClient provider = providerClient(List.of(), providerCalls);
        var adapter = adapter(store(new AtomicInteger()), reference -> Optional.empty(), provider,
                bridge(200), new FlespiAdapterState());
        adapter.pollNow();
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void authenticationFailureIsBoundedAndDoesNotAdvanceWatermark() {
        AtomicInteger lookups = new AtomicInteger();
        var adapter = adapter(store(lookups), reference -> Optional.empty(),
                providerClient(List.of(), new AtomicInteger()), bridge(200), new FlespiAdapterState());
        adapter.pollNow();
        assertThat(adapter.watermark()).isNull();
        assertThat(adapter.retryAttempt()).isZero();
        assertThat(adapter.nextAttempt()).isEqualTo(Instant.MAX);
    }

    @Test
    void concurrentPollForSameTargetIsSkipped() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        var provider = new FlespiProviderClient(properties, json, (uri, authorization, timeout) -> {
            calls.incrementAndGet();
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return new FlespiProviderClient.HttpResult(200,
                    "{\"result\":[]}".getBytes(StandardCharsets.UTF_8));
        });
        var adapter = adapter(store(new AtomicInteger()), reference -> Optional.of(SECRET.clone()),
                provider, bridge(200), new FlespiAdapterState());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(adapter::pollNow);
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            adapter.pollNow();
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
        }
        assertThat(calls).hasValue(1);
    }

    private Harness harness(List<JsonNode> messages, int ingressStatus) {
        AtomicInteger bindingLookups = new AtomicInteger();
        AtomicInteger secretLookups = new AtomicInteger();
        TrackingStore store = store(bindingLookups);
        IntegrationSecretResolver secrets = reference -> {
            secretLookups.incrementAndGet();
            return Optional.of(SECRET.clone());
        };
        FlespiProviderClient provider = providerClient(messages, new AtomicInteger());
        var bridge = bridge(ingressStatus);
        var state = new FlespiAdapterState();
        return new Harness(adapter(store, secrets, provider, bridge, state), state,
                bindingLookups, secretLookups);
    }

    private FlespiProviderClient providerClient(List<JsonNode> messages, AtomicInteger calls) {
        return new FlespiProviderClient(properties, json, (uri, authorization, timeout) -> {
            calls.incrementAndGet();
            byte[] body = json.writeValueAsBytes(java.util.Map.of("result", messages));
            return new FlespiProviderClient.HttpResult(200, body);
        });
    }

    private TrackingIngressBridge bridge(int status) {
        return new TrackingIngressBridge(properties, json, Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry(), request -> status, new SecureRandom());
    }

    private TrackingStore store(AtomicInteger lookups) {
        return (TrackingStore) java.lang.reflect.Proxy.newProxyInstance(
                TrackingStore.class.getClassLoader(), new Class<?>[] {TrackingStore.class}, (proxy, method, args) -> {
                    if (method.getName().equals("providerBinding")) {
                        lookups.incrementAndGet();
                        return Optional.of(binding());
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private FlespiPollingAdapter adapter(TrackingStore store, IntegrationSecretResolver secrets,
                                         FlespiProviderClient provider, TrackingIngressBridge bridge,
                                         FlespiAdapterState state) {
        return new FlespiPollingAdapter(properties, store, secrets, provider,
                new FlespiMessageMapper(properties), bridge, state, new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ProviderBinding binding() {
        return new ProviderBinding(UUID.randomUUID(), TENANT, "flespi-key-1", "FLESPI",
                "env:FLESPI_TOKEN", ProviderBindingLifecycle.ACTIVE, NOW, UUID.randomUUID(),
                NOW, UUID.randomUUID(), 0);
    }

    private JsonNode fixture() throws Exception {
        try (var stream = getClass().getResourceAsStream("/tracking/flespi/fmc130-documentation-aligned.json")) {
            return json.readTree(stream);
        }
    }

    private FlespiMessageMapper.MappedPosition mappedPosition() {
        return new FlespiMessageMapper.MappedPosition(DEVICE, NOW, new BigDecimal("6.9"),
                new BigDecimal("79.8"), null, null, null, java.util.Map.of("source", "flespi-rest"));
    }

    private void assertFailure(int status, String body, FlespiFailure.Kind kind) {
        var client = new FlespiProviderClient(properties, json, (uri, authorization, timeout) ->
                new FlespiProviderClient.HttpResult(status, body.getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> client.fetch(SECRET, NOW.minusSeconds(1), NOW))
                .isInstanceOfSatisfying(FlespiFailure.class, failure -> assertThat(failure.kind()).isEqualTo(kind));
    }

    private static String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private record Harness(FlespiPollingAdapter adapter, FlespiAdapterState state,
                           AtomicInteger bindingLookups, AtomicInteger secretLookups) {}
}
