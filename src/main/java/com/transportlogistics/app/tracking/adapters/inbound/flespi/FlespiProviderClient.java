package com.transportlogistics.app.tracking.adapters.inbound.flespi;

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
import org.springframework.stereotype.Component;

@Component
final class FlespiProviderClient {
    private static final String FIELDS = "ident,timestamp,position.latitude,position.longitude,"
            + "position.accuracy,position.speed,position.direction";
    private final ObjectMapper json;
    private final HttpTransport transport;

    @org.springframework.beans.factory.annotation.Autowired
    FlespiProviderClient(ObjectMapper json) {
        this(json, new JdkHttpTransport());
    }

    FlespiProviderClient(ObjectMapper json, HttpTransport transport) {
        this.json = json;
        this.transport = transport;
    }

    void testConnection(URI endpoint, char[] token, Duration timeout) {
        URI uri = endpoint.resolve("/gw/devices/all?fields=id&limit=1");
        exchange(uri, token, timeout, FlespiTrackingProviderAdapter.MAXIMUM_RESPONSE_BYTES);
    }

    List<JsonNode> fetch(
            URI endpoint,
            String externalDeviceReference,
            char[] token,
            Instant from,
            Instant to,
            int pageLimit,
            int responseByteLimit,
            Duration timeout) {
        URI uri = requestUri(endpoint, externalDeviceReference, from, to, pageLimit);
        HttpResult result = exchange(uri, token, timeout, responseByteLimit);
        try {
            JsonNode root = json.readTree(result.body());
            JsonNode messages = root.isArray() ? root : root.path("result");
            if (!messages.isArray()) {
                throw new FlespiFailure(FlespiFailure.Kind.PERMANENT,
                        "provider_response_malformed", null);
            }
            List<JsonNode> bounded = new ArrayList<>();
            messages.forEach(message -> {
                if (bounded.size() >= pageLimit) {
                    throw new FlespiFailure(FlespiFailure.Kind.PERMANENT,
                            "provider_page_oversize", null);
                }
                bounded.add(message);
            });
            return List.copyOf(bounded);
        } catch (FlespiFailure exception) {
            throw exception;
        } catch (IOException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.PERMANENT,
                    "provider_response_malformed", exception);
        }
    }

    URI requestUri(
            URI endpoint,
            String externalDeviceReference,
            Instant from,
            Instant to,
            int pageLimit) {
        String path = "/gw/devices/" + encode(externalDeviceReference) + "/messages";
        String query = "data=" + encode(FIELDS) + "&from=" + from.getEpochSecond()
                + "&to=" + to.getEpochSecond() + "&count=" + pageLimit;
        return endpoint.resolve(path + "?" + query);
    }

    private HttpResult exchange(URI uri, char[] token, Duration timeout, int responseByteLimit) {
        try {
            HttpResult result = transport.get(
                    uri, "FlespiToken " + new String(token), timeout, responseByteLimit);
            if (result.status() == 401 || result.status() == 403) {
                throw new FlespiFailure(FlespiFailure.Kind.AUTHENTICATION,
                        "provider_authentication", null);
            }
            if (result.status() == 429 || result.status() >= 500) {
                throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT,
                        result.status() == 429 ? "provider_rate_limited" : "provider_transient", null);
            }
            if (result.status() < 200 || result.status() >= 300) {
                throw new FlespiFailure(FlespiFailure.Kind.PERMANENT,
                        "provider_rejected", null);
            }
            if (result.body().length > responseByteLimit) {
                throw new FlespiFailure(FlespiFailure.Kind.PERMANENT,
                        "provider_response_oversize", null);
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT,
                    "provider_interrupted", exception);
        } catch (IOException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT,
                    "provider_unavailable", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    interface HttpTransport {
        HttpResult get(URI uri, String authorization, Duration timeout, int responseByteLimit)
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
                URI uri, String authorization, Duration timeout, int responseByteLimit)
                throws IOException, InterruptedException {
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IOException("TLS is required");
            }
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                    .header("Authorization", authorization)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<java.io.InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                byte[] body = stream.readNBytes(responseByteLimit + 1);
                return new HttpResult(response.statusCode(), body);
            }
        }
    }
}
