package com.transportlogistics.app.tracking.application.provider;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record ProviderSafeConfiguration(Map<String, String> values) {
    private static final int MAX_ENTRIES = 64;
    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_VALUE_LENGTH = 500;
    private static final int MAX_BYTES = 8_192;
    private static final Pattern KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,63}");
    private static final Pattern SECRET_KEY = Pattern.compile(
            ".*(token|secret|password|credential|private[_.-]?key|api[_.-]?key).*",
            Pattern.CASE_INSENSITIVE);

    public ProviderSafeConfiguration {
        Objects.requireNonNull(values, "values");
        if (values.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Safe configuration exceeds 64 entries");
        }
        TreeMap<String, String> copy = new TreeMap<>();
        int bytes = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                throw new IllegalArgumentException("Safe configuration cannot contain null");
            }
            if (key.length() > MAX_KEY_LENGTH || !KEY.matcher(key).matches()
                    || SECRET_KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("Unsafe configuration key");
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException("Safe configuration value exceeds 500 characters");
            }
            bytes += key.getBytes(StandardCharsets.UTF_8).length;
            bytes += value.getBytes(StandardCharsets.UTF_8).length;
            copy.put(key, value);
        }
        if (bytes > MAX_BYTES) {
            throw new IllegalArgumentException("Safe configuration exceeds 8192 bytes");
        }
        values = Map.copyOf(copy);
    }

    public static ProviderSafeConfiguration empty() {
        return new ProviderSafeConfiguration(Map.of());
    }
}
