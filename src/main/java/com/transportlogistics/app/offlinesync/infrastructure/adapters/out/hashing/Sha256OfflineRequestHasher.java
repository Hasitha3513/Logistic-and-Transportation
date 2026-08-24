package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.hashing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase.OperationCommand;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineRequestHasher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class Sha256OfflineRequestHasher implements OfflineRequestHasher {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    private static final Pattern OFFSET_TIMESTAMP_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$");

    @Override
    public String hash(OperationCommand operation) {
        String canonical = "{" +
                quote("aggregateId") + ":" + quote(operation.aggregateId().toString().toLowerCase()) + "," +
                quote("aggregateType") + ":" + quote(operation.aggregateType()) + "," +
                quote("operationType") + ":" + quote(operation.operationType()) + "," +
                quote("operationVersion") + ":" + operation.operationVersion() + "," +
                quote("payload") + ":" + canonical(operation.payload()) +
                "}";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonical(JsonNode node) {
        if (node == null || node.isNull()) {
            return "null";
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            object.fieldNames().forEachRemaining(names::add);
            names.sort(Comparator.naturalOrder());
            return names.stream()
                    .filter(name -> !object.get(name).isNull())
                    .map(name -> quote(name) + ":" + canonical(object.get(name)))
                    .reduce((left, right) -> left + "," + right)
                    .map(value -> "{" + value + "}")
                    .orElse("{}");
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(value -> values.add(canonical(value)));
            return "[" + String.join(",", values) + "]";
        }
        if (node.isNumber()) {
            BigDecimal number = node.decimalValue().stripTrailingZeros();
            return number.signum() == 0 ? "0" : number.toPlainString();
        }
        if (node.isBoolean()) {
            return Boolean.toString(node.booleanValue());
        }
        return quote(normalizeText(node.textValue()));
    }

    private String normalizeText(String value) {
        if (UUID_PATTERN.matcher(value).matches()) {
            return UUID.fromString(value).toString();
        }
        if (OFFSET_TIMESTAMP_PATTERN.matcher(value).matches()) {
            try {
                return OffsetDateTime.parse(value).toInstant().toString();
            } catch (DateTimeParseException ignored) {
                return value;
            }
        }
        return value;
    }

    private String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
