package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
final class TrackingIngressBridge {
    private final FlespiAdapterProperties properties;
    private final ObjectMapper json;
    private final Clock clock;
    private final MeterRegistry meters;
    private final IngressTransport transport;
    private final SecureRandom secureRandom;

    @org.springframework.beans.factory.annotation.Autowired
    TrackingIngressBridge(FlespiAdapterProperties properties, ObjectMapper json, Clock clock,
                          MeterRegistry meters) {
        this(properties, json, clock, meters, new JdkIngressTransport(properties), new SecureRandom());
    }

    TrackingIngressBridge(FlespiAdapterProperties properties, ObjectMapper json, Clock clock,
                          MeterRegistry meters, IngressTransport transport, SecureRandom secureRandom) {
        this.properties = properties;
        this.json = json;
        this.clock = clock;
        this.meters = meters;
        this.transport = transport;
        this.secureRandom = secureRandom;
    }

    void ingest(List<FlespiMessageMapper.MappedPosition> positions, char[] secret) {
        if (positions.isEmpty() || positions.size() > FlespiAdapterProperties.MAXIMUM_PAGE_SIZE) {
            throw new FlespiFailure(FlespiFailure.Kind.DOWNSTREAM, "ingress_batch_invalid", null);
        }
        String body = serialize(positions);
        if (body.getBytes(StandardCharsets.UTF_8).length > FlespiAdapterProperties.MAXIMUM_RESPONSE_BYTES) {
            throw new FlespiFailure(FlespiFailure.Kind.DOWNSTREAM, "ingress_batch_oversize", null);
        }
        long epoch = clock.instant().getEpochSecond();
        String nonce = nonce();
        String canonical = epoch + "\n" + nonce + "\n" + properties.getProviderKeyId() + "\n"
                + properties.getProviderAlias() + "\n" + body;
        String signature = hmac(secret, canonical);
        Timer.Sample sample = Timer.start(meters);
        try {
            int status = transport.post(new SignedIngressRequest(body, epoch, nonce, signature));
            if (status < 200 || status >= 300) {
                FlespiFailure.Kind kind = status >= 500 || status == 429
                        ? FlespiFailure.Kind.TRANSIENT : FlespiFailure.Kind.DOWNSTREAM;
                throw new FlespiFailure(kind, "ingress_rejected", null);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "ingress_interrupted", exception);
        } catch (IOException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.TRANSIENT, "ingress_unavailable", exception);
        } finally {
            sample.stop(meters.timer("tracking.flespi.ingress.latency", "provider", safeProvider()));
        }
    }

    private String serialize(List<FlespiMessageMapper.MappedPosition> positions) {
        ArrayNode array = json.createArrayNode();
        for (var position : positions) {
            ObjectNode node = array.addObject();
            node.put("deviceId", position.deviceId().toString());
            node.put("sourceTimestamp", position.sourceTimestamp().toString());
            node.put("latitude", position.latitude());
            node.put("longitude", position.longitude());
            put(node, "horizontalAccuracyMeters", position.horizontalAccuracyMeters());
            put(node, "speedKph", position.speedKph());
            put(node, "headingDegrees", position.headingDegrees());
            node.set("safeMetadata", json.valueToTree(position.safeMetadata()));
        }
        try {
            return json.writeValueAsString(array);
        } catch (JsonProcessingException exception) {
            throw new FlespiFailure(FlespiFailure.Kind.MAPPING, "normalized_serialization", exception);
        }
    }

    private static void put(ObjectNode node, String field, java.math.BigDecimal value) {
        if (value != null) node.put(field, value);
    }

    private String nonce() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private static String hmac(char[] secret, String canonical) {
        byte[] key = new String(secret).getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", exception);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private String safeProvider() {
        String value = properties.getProviderAlias();
        return value == null || value.isBlank() ? "UNKNOWN" : value.toUpperCase(java.util.Locale.ROOT);
    }

    interface IngressTransport {
        int post(SignedIngressRequest request) throws IOException, InterruptedException;
    }

    record SignedIngressRequest(String body, long epoch, String nonce, String signature) {}

    private static final class JdkIngressTransport implements IngressTransport {
        private final FlespiAdapterProperties properties;
        private final HttpClient client;

        private JdkIngressTransport(FlespiAdapterProperties properties) {
            this.properties = properties;
            client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }

        @Override
        public int post(SignedIngressRequest signed) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(properties.getTrackingIngressUrl())
                    .timeout(properties.getRequestTimeout()).header("Content-Type", "application/json")
                    .header("X-Tracking-Provider-Key-Id", properties.getProviderKeyId())
                    .header("X-Tracking-Provider", properties.getProviderAlias())
                    .header("X-Tracking-Timestamp", Long.toString(signed.epoch()))
                    .header("X-Tracking-Nonce", signed.nonce())
                    .header("X-Tracking-Signature", signed.signature())
                    .POST(HttpRequest.BodyPublishers.ofString(signed.body(), StandardCharsets.UTF_8)).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }
}
