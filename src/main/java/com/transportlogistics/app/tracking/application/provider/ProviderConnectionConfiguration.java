package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

public record ProviderConnectionConfiguration(
        ProviderType providerType,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration) {

    private static final Pattern SECRET_QUERY_KEY = Pattern.compile(
            "(^|&)(password|secret|token|api_?key|authorization|private_?key|credential)=",
            Pattern.CASE_INSENSITIVE);

    public ProviderConnectionConfiguration {
        Objects.requireNonNull(providerType, "providerType");
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        if (endpoint != null && (!endpoint.isAbsolute() || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null || containsSecretQuery(endpoint))) {
            throw new IllegalArgumentException("Provider endpoint must be an absolute credential-free URI");
        }
    }

    private static boolean containsSecretQuery(URI endpoint) {
        String query = endpoint.getRawQuery();
        return query != null && SECRET_QUERY_KEY.matcher(query).find();
    }
}
