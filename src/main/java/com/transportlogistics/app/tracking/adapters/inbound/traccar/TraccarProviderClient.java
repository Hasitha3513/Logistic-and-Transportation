package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
final class TraccarProviderClient {
    private final ObjectMapper json;
    private final TraccarEndpointPolicy endpoints;
    private final HttpTransport transport;

    @Autowired
    TraccarProviderClient(ObjectMapper json, TraccarEndpointPolicy endpoints) {
        this(json, endpoints, new JdkHttpTransport());
    }

    TraccarProviderClient(
            ObjectMapper json, TraccarEndpointPolicy endpoints, HttpTransport transport) {
        this.json = json;
        this.endpoints = endpoints;
        this.transport = transport;
    }

    void testConnection(URI endpoint, char[] token, Duration timeout) {
        exchange(endpoint.resolve("/api/server"), token, timeout,
                TraccarTrackingProviderAdapter.MAXIMUM_RESPONSE_BYTES);
    }

    List<JsonNode> fetch(
            URI endpoint, String deviceId, char[] token, Instant from, Instant to,
            int recordLimit, int byteLimit, Duration timeout) {
        URI request = requestUri(endpoint, deviceId, from, to);
        HttpResult result = exchange(request, token, timeout, byteLimit);
        try {
            JsonNode root = json.readTree(result.body());
            if (!root.isArray()) {
                throw malformed();
            }
            List<JsonNode> positions = new ArrayList<>();
            root.forEach(position -> {
                if (positions.size() >= recordLimit) {
                    throw new TraccarFailure(
                            TraccarFailure.Kind.PERMANENT, "provider_response_overflow", null);
                }
                positions.add(position);
            });
            return List.copyOf(positions);
        } catch (TraccarFailure exception) {
            throw exception;
        } catch (IOException exception) {
            throw malformed();
        }
    }

    URI requestUri(URI endpoint, String deviceId, Instant from, Instant to) {
        if (deviceId == null || !deviceId.matches("[1-9][0-9]{0,18}")) {
            throw new TraccarFailure(
                    TraccarFailure.Kind.PERMANENT, "provider_device_reference_invalid", null);
        }
        String query = "deviceId=" + encode(deviceId) + "&from=" + encode(from.toString())
                + "&to=" + encode(to.toString());
        return endpoint.resolve("/api/positions?" + query);
    }

    private HttpResult exchange(URI uri, char[] token, Duration timeout, int byteLimit) {
        endpoints.authorizeConnection(base(uri));
        try {
            HttpResult result = transport.get(
                    uri, "Bearer " + new String(token), timeout, byteLimit);
            if (result.status() == 401 || result.status() == 403) {
                throw new TraccarFailure(
                        TraccarFailure.Kind.AUTHENTICATION, "provider_authentication", null);
            }
            if (result.status() == 429 || result.status() >= 500) {
                throw new TraccarFailure(TraccarFailure.Kind.TRANSIENT,
                        result.status() == 429 ? "provider_rate_limited" : "provider_transient", null);
            }
            if (result.status() < 200 || result.status() >= 300) {
                throw new TraccarFailure(
                        TraccarFailure.Kind.PERMANENT, "provider_rejected", null);
            }
            if (result.body().length > byteLimit) {
                throw new TraccarFailure(
                        TraccarFailure.Kind.PERMANENT, "provider_response_overflow", null);
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TraccarFailure(
                    TraccarFailure.Kind.TRANSIENT, "provider_interrupted", exception);
        } catch (IOException exception) {
            throw new TraccarFailure(
                    TraccarFailure.Kind.TRANSIENT, "provider_unavailable", exception);
        }
    }

    private static URI base(URI uri) {
        return URI.create(uri.getScheme() + "://" + uri.getAuthority());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static TraccarFailure malformed() {
        return new TraccarFailure(
                TraccarFailure.Kind.MAPPING, "provider_response_malformed", null);
    }

    interface HttpTransport {
        HttpResult get(URI uri, String authorization, Duration timeout, int byteLimit)
                throws IOException, InterruptedException;
    }

    record HttpResult(int status, byte[] body) { }

    private static final class JdkHttpTransport implements HttpTransport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        @Override
        public HttpResult get(
                URI uri, String authorization, Duration timeout, int byteLimit)
                throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                    .header("Authorization", authorization)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<java.io.InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                return new HttpResult(response.statusCode(), stream.readNBytes(byteLimit + 1));
            }
        }
    }
}
