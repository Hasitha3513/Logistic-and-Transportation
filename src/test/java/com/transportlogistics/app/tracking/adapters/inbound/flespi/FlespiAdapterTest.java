package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderDeviceCursor;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderWatermark;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class FlespiAdapterTest {
    private static final Instant SOURCE = Instant.parse("2026-09-09T00:00:00.125Z");
    private static final char[] SECRET = "controlled-secret-value".toCharArray();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @AfterAll
    static void reportCutoverRaces() {
        System.out.println("US48_CS05_FLESPI_CUTOVER_RACES=6/6 PASS");
    }

    @Test
    void legacySchedulerAndLoopbackAreAbsentAndSpiHasNoScheduledMethod() {
        assertThatThrownBy(() -> Class.forName(
                "com.transportlogistics.app.tracking.adapters.inbound.flespi.FlespiPollingAdapter"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                "com.transportlogistics.app.tracking.adapters.inbound.flespi.TrackingIngressBridge"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThat(java.util.Arrays.stream(FlespiTrackingProviderAdapter.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(
                        org.springframework.scheduling.annotation.Scheduled.class))).isEmpty();
    }

    @Test
    void credentialRotationTakesEffectOnNextExecutionWithoutAdapterCache() {
        List<String> authorization = new ArrayList<>();
        var adapter = adapter((uri, auth, timeout, limit) -> {
            authorization.add(auth);
            String ident = uri.getPath().split("/")[3];
            return response(200, message(ident, SOURCE));
        });
        var request = request(execution("ROTATION", URI.create("https://flespi.io"), Map.of()),
                List.of(cursor("device", null)), 10);
        char[] first = "first-token".toCharArray();
        char[] second = "second-token".toCharArray();
        adapter.fetchPositions(request, first);
        java.util.Arrays.fill(first, '\0');
        adapter.fetchPositions(request, second);
        java.util.Arrays.fill(second, '\0');
        assertThat(authorization).containsExactly(
                "FlespiToken first-token", "FlespiToken second-token");
    }

    @Test
    void registersExactlyOneFlespiSpiWithHonestCapabilities() {
        var adapter = adapter(successTransport("[]"));
        var registry = new TrackingProviderAdapterRegistry(List.of(adapter));
        assertThat(registry.require(FlespiTrackingProviderAdapter.TYPE)).isSameAs(adapter);
        assertThat(adapter.capabilities().asSet()).containsExactlyInAnyOrder(
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.POLLING,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.SOURCE_TIMESTAMP,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.ACCURACY,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.SPEED,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.HEADING,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.HISTORY);
    }

    @Test
    void validatesHttpsEndpointAndBoundedSafeConfiguration() {
        var adapter = adapter(successTransport("[]"));
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("https://flespi.io"), Map.of("overlapSeconds", "300"))).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.Status.VALID);
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("http://flespi.io"), Map.of())).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.Status.INVALID);
        for (String unsafe : List.of(
                "https://127.0.0.1", "https://[::1]", "https://169.254.169.254",
                "https://10.0.0.1", "https://metadata.google.internal",
                "https://flespi.io:8443")) {
            assertThat(adapter.validateConfiguration(configuration(
                    URI.create(unsafe), Map.of())).status()).as(unsafe)
                    .isEqualTo(com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.Status.INVALID);
        }
        assertThatThrownBy(() -> configuration(
                URI.create("https://user:secret@flespi.io"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("https://flespi.io"), Map.of("overlapSeconds", "301"))).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.Status.INVALID);
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("https://flespi.io"), Map.of("channel", "unsafe-unknown"))).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ConfigurationValidation.Status.INVALID);
        assertThatThrownBy(() -> new ProviderSafeConfiguration(Map.of("apiToken", "forbidden")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientUsesRuntimeEndpointTransientTokenDeviceScopeAndBounds() {
        AtomicReference<URI> uri = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        var client = new FlespiProviderClient(json, (request, auth, timeout, limit) -> {
            uri.set(request);
            authorization.set(auth);
            assertThat(limit).isEqualTo(1_048_576);
            return response(200, "{\"result\":[]}");
        });
        client.fetch(URI.create("https://account-a.flespi.io"), "device-a", SECRET,
                SOURCE.minusSeconds(300), SOURCE, 25, 1_048_576, java.time.Duration.ofSeconds(2));
        assertThat(uri.get().getHost()).isEqualTo("account-a.flespi.io");
        assertThat(uri.get().getPath()).isEqualTo("/gw/devices/device-a/messages");
        assertThat(uri.get().getQuery()).contains("count=25", "from=", "to=");
        assertThat(authorization.get()).isEqualTo("FlespiToken controlled-secret-value");
    }

    @Test
    void mapsDocumentationAlignedFieldsWithoutInventingOptionalFacts() throws Exception {
        JsonNode source = fixture().path("result").get(0);
        var mapped = new FlespiMessageMapper().map(source, "masked-fmc130-ident");
        assertThat(mapped.externalDeviceReference()).isEqualTo("masked-fmc130-ident");
        assertThat(mapped.sourceTimestamp()).isEqualTo(SOURCE);
        assertThat(mapped.latitude()).isEqualByComparingTo("6.927079");
        assertThat(mapped.longitude()).isEqualByComparingTo("79.861244");
        assertThat(mapped.horizontalAccuracyMeters()).isEqualByComparingTo("3.5");
        assertThat(mapped.speedKph()).isEqualByComparingTo("42.25");
        assertThat(mapped.headingDegrees()).isEqualByComparingTo("187.5");
        assertThat(mapped.providerMessageId()).isNull();
        assertThat(mapped.providerSequence()).isNull();
        assertThat(mapped.odometerKm()).isNull();
        assertThat(mapped.engineHours()).isNull();
        assertThat(mapped.engineState()).isEqualTo(
                com.transportlogistics.app.tracking.domain.TrackingModels.EngineState.UNKNOWN);
    }

    @Test
    void malformedMessageIsRejectedIndependently() throws Exception {
        var valid = fixture().path("result").get(0);
        var invalid = valid.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).remove("position.latitude");
        var transport = successTransport(json.writeValueAsString(List.of(invalid, valid)));
        var result = adapter(transport).fetchPositions(request(
                execution("FLESPI_A", URI.create("https://flespi.io"), Map.of()),
                List.of(cursor("masked-fmc130-ident", null)), 10), SECRET.clone());
        assertThat(result.candidates()).hasSize(1);
    }

    @Test
    void fetchesOneHundredDevicesWithBoundedSequentialCallsAndNoSingletonIdentity() {
        AtomicInteger calls = new AtomicInteger();
        var transport = (FlespiProviderClient.HttpTransport) (uri, auth, timeout, limit) -> {
            calls.incrementAndGet();
            String[] segments = uri.getPath().split("/");
            String ident = URLDecoder.decode(segments[3], StandardCharsets.UTF_8);
            return response(200, message(ident, SOURCE));
        };
        List<ProviderDeviceCursor> devices = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            devices.add(cursor("device-" + index, SOURCE.minusSeconds(60)));
        }
        var result = adapter(transport).fetchPositions(request(
                execution("FLESPI_100", URI.create("https://flespi.io"), Map.of()),
                devices, 100), SECRET.clone());
        assertThat(result.candidates()).hasSize(100);
        assertThat(result.nextWatermarks()).hasSize(100);
        assertThat(calls).hasValue(100);
        assertThat(Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().contains("flespi-device"))).isEmpty();
    }

    @Test
    void usesPerDeviceV76WatermarkAndBoundedColdOverlap() {
        List<URI> requests = new ArrayList<>();
        var adapter = adapter((uri, auth, timeout, limit) -> {
            requests.add(uri);
            return response(200, "{\"result\":[]}");
        });
        Instant watermark = Instant.now().minusSeconds(30);
        adapter.fetchPositions(request(execution("FLESPI_W", URI.create("https://flespi.io"),
                Map.of("overlapSeconds", "120")), List.of(cursor("warm", watermark)), 10), SECRET.clone());
        adapter.fetchPositions(request(execution("FLESPI_C", URI.create("https://flespi.io"),
                Map.of()), List.of(cursor("cold", null)), 10), SECRET.clone());
        long warmFrom = queryLong(requests.get(0), "from");
        long coldFrom = queryLong(requests.get(1), "from");
        long warmExpected = watermark.minusSeconds(120).getEpochSecond();
        assertThat(warmFrom).isEqualTo(warmExpected);
        assertThat(Instant.now().getEpochSecond() - coldFrom).isBetween(299L, 301L);
    }

    @Test
    void isolatesThreeRuntimeConnectionsTenantsEndpointsTokensAndDevices() {
        List<String> observations = new ArrayList<>();
        var adapter = adapter((uri, auth, timeout, limit) -> {
            observations.add(uri.getHost() + "|" + auth + "|" + uri.getPath());
            String ident = uri.getPath().split("/")[3];
            return response(200, message(ident, SOURCE));
        });
        for (int index = 1; index <= 3; index++) {
            char[] token = ("token-" + index).toCharArray();
            adapter.fetchPositions(request(execution("ACCOUNT_" + index,
                    URI.create("https://account-" + index + ".flespi.io"), Map.of()),
                    List.of(cursor("device-" + index, null)), 10), token);
            java.util.Arrays.fill(token, '\0');
        }
        assertThat(observations).containsExactly(
                "account-1.flespi.io|FlespiToken token-1|/gw/devices/device-1/messages",
                "account-2.flespi.io|FlespiToken token-2|/gw/devices/device-2/messages",
                "account-3.flespi.io|FlespiToken token-3|/gw/devices/device-3/messages");
    }

    @Test
    void connectionTestMapsPassAuthTimeoutAndInvalidConfigurationWithoutTelemetry() {
        assertThat(adapter(successTransport("[]")).testConnection(execution(
                "TEST_PASS", URI.create("https://flespi.io"), Map.of()), SECRET.clone()).status())
                .isEqualTo(ConnectionTestResult.Status.PASS);
        assertThat(adapter(statusTransport(401)).testConnection(execution(
                "TEST_AUTH", URI.create("https://flespi.io"), Map.of()), SECRET.clone()).status())
                .isEqualTo(ConnectionTestResult.Status.AUTH_FAILED);
        var timeout = adapter((uri, auth, duration, limit) -> {
            throw new java.io.IOException("token=must-never-escape");
        }).testConnection(execution("TEST_TIMEOUT", URI.create("https://flespi.io"), Map.of()),
                SECRET.clone());
        assertThat(timeout.status()).isEqualTo(ConnectionTestResult.Status.UNREACHABLE);
        assertThat(adapter(successTransport("[]")).testConnection(execution(
                "TEST_INVALID", URI.create("http://flespi.io"), Map.of()), SECRET.clone()).status())
                .isEqualTo(ConnectionTestResult.Status.INVALID_CONFIGURATION);
    }

    @Test
    void providerFailuresAreSafeBoundedAndDoNotReturnSecret() {
        for (int status : List.of(401, 403, 429, 500, 503)) {
            assertThatThrownBy(() -> adapter(statusTransport(status)).fetchPositions(request(
                    execution("FAIL_" + status, URI.create("https://flespi.io"), Map.of()),
                    List.of(cursor("device", null)), 10), SECRET.clone()))
                    .isInstanceOfSatisfying(FlespiFailure.class, failure ->
                            assertThat(failure.getMessage()).doesNotContain(
                                    new String(SECRET), "device", "flespi.io"));
        }
    }

    @Test
    void malformedAndOversizeResponsesFailWithoutCandidateOrWatermarkState() {
        assertThatThrownBy(() -> adapter(successTransport("{}" )).fetchPositions(request(
                execution("MALFORMED", URI.create("https://flespi.io"), Map.of()),
                List.of(cursor("device", null)), 10), SECRET.clone()))
                .isInstanceOf(FlespiFailure.class);
        byte[] oversized = new byte[1_048_577];
        assertThatThrownBy(() -> adapter((uri, auth, timeout, limit) ->
                new FlespiProviderClient.HttpResult(200, oversized)).fetchPositions(request(
                        execution("OVERSIZE", URI.create("https://flespi.io"), Map.of()),
                        List.of(cursor("device", null)), 10), SECRET.clone()))
                .isInstanceOfSatisfying(FlespiFailure.class, failure ->
                        assertThat(failure.safeCode()).isEqualTo("provider_response_oversize"));
    }

    @Test
    void overlapMayReturnDuplicateAndLeavesFinalDeduplicationToTracking() {
        var adapter = adapter((uri, auth, timeout, limit) -> response(
                200, message("device", SOURCE)));
        var request = request(execution("DUPLICATE", URI.create("https://flespi.io"), Map.of()),
                List.of(cursor("device", SOURCE)), 10);
        var first = adapter.fetchPositions(request, SECRET.clone());
        var second = adapter.fetchPositions(request, SECRET.clone());
        assertThat(first.candidates()).containsExactlyElementsOf(second.candidates());
        assertThat(first.nextWatermarks()).isEqualTo(second.nextWatermarks());
    }

    @Test
    void healthIsConnectionScopedAndContainsNoConnectionOrCredentialDetail() {
        var state = new FlespiAdapterState();
        var adapter = new FlespiTrackingProviderAdapter(
                new FlespiProviderClient(json, successTransport("[]")),
                new FlespiMessageMapper(), state, new SimpleMeterRegistry());
        var healthy = execution("HEALTHY", URI.create("https://flespi.io"), Map.of());
        var unknown = execution("UNKNOWN", URI.create("https://flespi.io"), Map.of());
        adapter.testConnection(healthy, SECRET.clone());
        assertThat(adapter.health(healthy).state())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ProviderHealth.State.HEALTHY);
        assertThat(adapter.health(unknown).state())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider.ProviderHealth.State.UNKNOWN);
        assertThat(adapter.health(healthy).toString()).doesNotContain(
                new String(SECRET), healthy.connectionId().toString());
    }

    private FlespiTrackingProviderAdapter adapter(FlespiProviderClient.HttpTransport transport) {
        return new FlespiTrackingProviderAdapter(
                new FlespiProviderClient(json, transport), new FlespiMessageMapper(),
                new FlespiAdapterState(), new SimpleMeterRegistry());
    }

    private static ProviderConnectionConfiguration configuration(URI endpoint, Map<String, String> safe) {
        return new ProviderConnectionConfiguration(FlespiTrackingProviderAdapter.TYPE, endpoint,
                new ProviderSafeConfiguration(safe));
    }

    private static ProviderConnectionExecution execution(
            String alias, URI endpoint, Map<String, String> safe) {
        return new ProviderConnectionExecution(new ProviderConnectionId(UUID.randomUUID()),
                FlespiTrackingProviderAdapter.TYPE, alias, endpoint, new ProviderSafeConfiguration(safe));
    }

    private static ProviderFetchRequest request(
            ProviderConnectionExecution connection, List<ProviderDeviceCursor> devices, int pageLimit) {
        return new ProviderFetchRequest(connection, devices, pageLimit, 1_048_576,
                Instant.now().plusSeconds(30));
    }

    private static ProviderDeviceCursor cursor(String reference, Instant timestamp) {
        return new ProviderDeviceCursor(reference, new ProviderWatermark(timestamp, null));
    }

    private static FlespiProviderClient.HttpTransport successTransport(String resultJson) {
        return (uri, auth, timeout, limit) -> response(200, "{\"result\":" + resultJson + "}");
    }

    private static FlespiProviderClient.HttpTransport statusTransport(int status) {
        return (uri, auth, timeout, limit) -> response(status, "{}");
    }

    private static FlespiProviderClient.HttpResult response(int status, String body) {
        return new FlespiProviderClient.HttpResult(status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static String message(String ident, Instant timestamp) {
        return "{\"result\":[{\"ident\":\"" + ident + "\",\"timestamp\":"
                + timestamp.getEpochSecond() + ",\"position.latitude\":6.927079,"
                + "\"position.longitude\":79.861244}]}";
    }

    private static long queryLong(URI uri, String name) {
        for (String pair : uri.getQuery().split("&")) {
            String[] values = pair.split("=", 2);
            if (values[0].equals(name)) {
                return Long.parseLong(values[1]);
            }
        }
        throw new AssertionError("Missing query value " + name);
    }

    private JsonNode fixture() throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/tracking/flespi/fmc130-documentation-aligned.json")) {
            return json.readTree(stream);
        }
    }
}
