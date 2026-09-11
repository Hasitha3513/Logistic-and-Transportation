package com.transportlogistics.app.integration.adapters.outbound.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.transportlogistics.app.integration.ports.outbound.IntegrationPayloadPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Component
class JacksonIntegrationPayloadAdapter implements IntegrationPayloadPort {
    private final ObjectMapper json;

    JacksonIntegrationPayloadAdapter(ObjectMapper json) { this.json = json; }

    @Override
    public String serialize(Map<String, ?> payload) {
        try {
            return json.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Canonical payload cannot be serialized", exception);
        }
    }

    @Override
    public String hash(String payload) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
