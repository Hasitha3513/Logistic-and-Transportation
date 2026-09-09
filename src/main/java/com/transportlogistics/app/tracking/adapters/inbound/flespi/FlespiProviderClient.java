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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
final class FlespiProviderClient {
    private static final String FIELDS = "ident,timestamp,position.latitude,position.longitude,"
            + "position.accuracy,position.speed,position.direction";
    private final FlespiAdapterProperties properties;
    private final ObjectMapper json;
    private final HttpTransport transport;

    @org.springframework.beans.factory.annotation.Autowired
    FlespiProviderClient(FlespiAdapterProperties properties, ObjectMapper json) {
        this(properties, json, new JdkHttpTransport(properties));
    }

    FlespiProviderClient(FlespiAdapterProperties properties, ObjectMapper json, HttpTransport transport) {
        this.properties = properties;
        this.json = json;
        this.transport = transport;
    }

    List<JsonNode> fetch(char[] token, Instant from, Instant to) {
        URI uri = requestUri(from, to);
        HttpResult result;
        try {
            result = transport.get(uri, "FlespiToken " + new String(token), properties.getRequestTimeout());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "provider_interrupted", exception);
        } catch (IOException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "provider_unavailable", exception);
        }
        if (result.status() == 401 || result.status() == 403) {
            throw new FlespiFailure(FlespiFailure.Kind.AUTHENTICATION, "provider_authentication", null);
        }
        if (result.status() == 429 || result.status() >= 500) {
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "provider_transient", null);
        }
        if (result.status() < 200 || result.status() >= 300) {
            throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "provider_rejected", null);
        }
        if (result.body().length > FlespiAdapterProperties.MAXIMUM_RESPONSE_BYTES) {
            throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "provider_response_oversize", null);
        }
        try {
            JsonNode root = json.readTree(result.body());
            JsonNode messages = root.isArray() ? root : root.path("result");
            if (!messages.isArray()) {
                throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "provider_response_malformed", null);
            }
            List<JsonNode> bounded = new ArrayList<>();
            messages.forEach(message -> {
                if (bounded.size() >= properties.getPageSize()) {
                    throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "provider_page_oversize", null);
                }
                bounded.add(message);
            });
            return List.copyOf(bounded);
        } catch (FlespiFailure exception) {
            throw exception;
        } catch (IOException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.PERMANENT, "provider_response_malformed", exception);
        }
    }

    URI requestUri(Instant from, Instant to) {
        String path = "/gw/devices/" + properties.getFlespiDeviceId() + "/messages";
        String query = "data=" + encode(FIELDS) + "&from=" + from.getEpochSecond()
                + "&to=" + to.getEpochSecond() + "&count=" + properties.getPageSize();
        return properties.getBaseUrl().resolve(path + "?" + query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    interface HttpTransport {
        HttpResult get(URI uri, String authorization, java.time.Duration timeout)
                throws IOException, InterruptedException;
    }

    record HttpResult(int status, byte[] body) {}

    private static final class JdkHttpTransport implements HttpTransport {
        private final HttpClient client;

        private JdkHttpTransport(FlespiAdapterProperties properties) {
            client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }

        @Override
        public HttpResult get(URI uri, String authorization, java.time.Duration timeout)
                throws IOException, InterruptedException {
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IOException("TLS is required");
            }
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                    .header("Authorization", authorization).header("Accept", "application/json").GET().build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                byte[] body = stream.readNBytes(FlespiAdapterProperties.MAXIMUM_RESPONSE_BYTES + 1);
                return new HttpResult(response.statusCode(), body);
            }
        }
    }
}
