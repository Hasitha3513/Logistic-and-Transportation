package com.transportlogistics.app.integration;

import java.util.Optional;

/** Provider-neutral secret lookup. Callers persist only the opaque reference. */
public interface IntegrationSecretResolver {
    Optional<char[]> resolve(String credentialReference);
}
