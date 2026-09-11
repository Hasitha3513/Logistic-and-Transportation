package com.transportlogistics.app.integration.adapters.outbound.secrets;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
class EnvironmentIntegrationSecretResolver implements IntegrationSecretResolver {
    private static final Pattern REFERENCE = Pattern.compile("env:[A-Z][A-Z0-9_]{0,126}");

    @Override
    public Optional<char[]> resolve(String credentialReference) {
        if (credentialReference == null || !REFERENCE.matcher(credentialReference).matches()) return Optional.empty();
        String value = System.getenv(credentialReference.substring(4));
        return value == null ? Optional.empty() : Optional.of(value.toCharArray());
    }
}
