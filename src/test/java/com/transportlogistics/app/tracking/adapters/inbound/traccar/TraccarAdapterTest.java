package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderDeviceCursor;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderWatermark;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TraccarAdapterTest {
    private static final Instant SOURCE = Instant.parse("2026-09-18T00:00:00Z");
    private static final char[] TOKEN = "opaque-runtime-token".toCharArray();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void registersOneProductionAdapterWithTruthfulCapabilities() throws Exception {
        var adapter = adapter(success("[]"), publicPolicy());
        var registry = new TrackingProviderAdapterRegistry(List.of(adapter));

        assertThat(registry.require(TraccarTrackingProviderAdapter.TYPE)).isSameAs(adapter);
        assertThat(adapter.capabilities().asSet()).containsExactlyInAnyOrder(
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.POLLING,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.SOURCE_TIMESTAMP,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.ACCURACY,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.SPEED,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.HEADING,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.IGNITION,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.ODOMETER,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.MESSAGE_ID,
                com.transportlogistics.app.tracking.application.provider.ProviderCapability.HISTORY);
    }

    @Test
    void usesBearerTimeRangeAndPreservesCanonicalSignalsAndWatermark() throws Exception {
        AtomicReference<URI> requestUri = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        var transport = (TraccarProviderClient.HttpTransport) (uri, auth, timeout, limit) -> {
            requestUri.set(uri);
            authorization.set(auth);
            return response(200, """
                    [{"id":42,"deviceId":7,"fixTime":"2026-09-18T00:00:00Z",
                      "latitude":6.927079,"longitude":79.861244,"speed":10.0,
                      "course":180.0,"accuracy":3.5,"altitude":12.0,
                      "attributes":{"ignition":true,"totalDistance":1234000,
                        "batteryLevel":75.5,"battery":12.6,"power":true,
                        "charge":false,"alarm":"tampering"}}]
                    """);
        };
        var adapter = adapter(transport, publicPolicy());
        var result = adapter.fetchPositions(request(), TOKEN.clone());

        assertThat(requestUri.get().getPath()).isEqualTo("/api/positions");
        assertThat(requestUri.get().getQuery()).contains(
                "deviceId=7", "from=", "to=");
        assertThat(authorization.get()).isEqualTo("Bearer opaque-runtime-token");
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.providerMessageId()).isEqualTo("42");
            assertThat(candidate.providerSequence()).isEqualTo(42L);
            assertThat(candidate.speedKph()).isEqualByComparingTo("18.520");
            assertThat(candidate.odometerKm()).isEqualByComparingTo("1234.000");
            assertThat(candidate.batteryLevelPercent()).isEqualByComparingTo("75.5");
            assertThat(candidate.tamperState().name()).isEqualTo("DETECTED");
        });
        assertThat(result.nextWatermarks().get("7"))
                .isEqualTo(new ProviderWatermark(SOURCE, "42"));
    }

    @Test
    void rejectsCompleteOversizedResponseWithoutSilentTruncation() throws Exception {
        String body = "[" + position(1) + "," + position(2) + "]";
        var client = new TraccarProviderClient(json, publicPolicy(), success(body));

        assertThatThrownBy(() -> client.fetch(
                URI.create("https://traccar.example"), "7", TOKEN.clone(),
                SOURCE.minusSeconds(60), SOURCE, 1, 1_048_576, Duration.ofSeconds(2)))
                .isInstanceOf(TraccarFailure.class)
                .hasMessage("provider_response_overflow");
    }

    @Test
    void validatesDnsEveryRequestAndBlocksRedirectLoopbackAndUnapprovedPrivateTargets()
            throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        var changing = new TraccarEndpointPolicy(Set.of(), host -> {
            int invocation = resolutions.incrementAndGet();
            return new InetAddress[] { InetAddress.getByName(
                    invocation == 1 ? "93.184.216.34" : "127.0.0.1") };
        });
        var client = new TraccarProviderClient(json, changing, success("[]"));
        client.testConnection(
                URI.create("https://traccar.example"), TOKEN.clone(), Duration.ofSeconds(1));
        assertThatThrownBy(() -> client.testConnection(
                URI.create("https://traccar.example"), TOKEN.clone(), Duration.ofSeconds(1)))
                .isInstanceOf(TraccarFailure.class)
                .hasMessage("provider_endpoint_blocked");

        var privateDenied = new TraccarEndpointPolicy(Set.of(), host ->
                new InetAddress[] { InetAddress.getByName("10.0.0.8") });
        assertThatThrownBy(() -> privateDenied.authorizeConnection(
                URI.create("https://internal.example"))).hasMessage("provider_endpoint_blocked");
        var privateAllowed = new TraccarEndpointPolicy(Set.of("internal.example:8443"), host ->
                new InetAddress[] { InetAddress.getByName("10.0.0.8") });
        privateAllowed.authorizeConnection(URI.create("https://internal.example:8443"));

        var redirect = new TraccarProviderClient(json, publicPolicy(),
                (uri, auth, timeout, limit) -> response(302, "[]"));
        assertThatThrownBy(() -> redirect.testConnection(
                URI.create("https://traccar.example"), TOKEN.clone(), Duration.ofSeconds(1)))
                .isInstanceOf(TraccarFailure.class).hasMessage("provider_rejected");
    }

    @Test
    void validatesConfigurationWithoutCredentialOrTenantControlledTrustExpansion() throws Exception {
        var adapter = adapter(success("[]"), publicPolicy());
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("https://traccar.example"), Map.of("overlapSeconds", "300"))).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider
                        .ConfigurationValidation.Status.VALID);
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("http://traccar.example"), Map.of())).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider
                        .ConfigurationValidation.Status.INVALID);
        assertThat(adapter.validateConfiguration(configuration(
                URI.create("https://traccar.example"), Map.of("allowPrivate", "true"))).status())
                .isEqualTo(com.transportlogistics.app.tracking.application.provider
                        .ConfigurationValidation.Status.INVALID);
        assertThatThrownBy(() -> new ProviderSafeConfiguration(Map.of("apiToken", "secret")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TraccarTrackingProviderAdapter adapter(
            TraccarProviderClient.HttpTransport transport, TraccarEndpointPolicy policy) {
        var client = new TraccarProviderClient(json, policy, transport);
        return new TraccarTrackingProviderAdapter(
                client, new TraccarMessageMapper(), policy,
                new TraccarAdapterState(), new SimpleMeterRegistry());
    }

    private ProviderFetchRequest request() {
        return new ProviderFetchRequest(
                new ProviderConnectionExecution(new ProviderConnectionId(UUID.randomUUID()),
                        TraccarTrackingProviderAdapter.TYPE, "TRACCAR_TEST",
                        URI.create("https://traccar.example"),
                        new ProviderSafeConfiguration(Map.of("overlapSeconds", "60"))),
                List.of(new ProviderDeviceCursor("7",
                        new ProviderWatermark(SOURCE.minusSeconds(30), "40"))),
                10, 1_048_576, Instant.now().plusSeconds(10));
    }

    private static ProviderConnectionConfiguration configuration(
            URI endpoint, Map<String, String> safe) {
        return new ProviderConnectionConfiguration(TraccarTrackingProviderAdapter.TYPE,
                endpoint, new ProviderSafeConfiguration(safe));
    }

    private static TraccarEndpointPolicy publicPolicy() throws Exception {
        return new TraccarEndpointPolicy(Set.of(), host ->
                new InetAddress[] { InetAddress.getByName("93.184.216.34") });
    }

    private static TraccarProviderClient.HttpTransport success(String body) {
        return (uri, auth, timeout, limit) -> response(200, body);
    }

    private static TraccarProviderClient.HttpResult response(int status, String body) {
        return new TraccarProviderClient.HttpResult(
                status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static String position(int id) {
        return "{\"id\":" + id + ",\"fixTime\":\"2026-09-18T00:00:00Z\","
                + "\"latitude\":6.9,\"longitude\":79.8}";
    }
}
