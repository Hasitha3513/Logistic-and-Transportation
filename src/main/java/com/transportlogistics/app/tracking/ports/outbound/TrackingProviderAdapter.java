package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.ConfigurationValidation;
import com.transportlogistics.app.tracking.application.provider.ConnectionTestResult;
import com.transportlogistics.app.tracking.application.provider.DiscoveryResult;
import com.transportlogistics.app.tracking.application.provider.FetchResult;
import com.transportlogistics.app.tracking.application.provider.ProviderCapabilities;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionConfiguration;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionExecution;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderDiscoveryRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderFetchRequest;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import com.transportlogistics.app.tracking.application.provider.ProviderType;

public interface TrackingProviderAdapter {
    ProviderType providerType();

    ProviderCapabilities capabilities();

    ConfigurationValidation validateConfiguration(
            ProviderConnectionConfiguration configuration);

    ConnectionTestResult testConnection(
            ProviderConnectionExecution connection, char[] secret);

    FetchResult fetchPositions(ProviderFetchRequest request, char[] secret);

    default DiscoveryResult discoverDevices(
            ProviderDiscoveryRequest request, char[] secret) {
        return DiscoveryResult.unsupported();
    }

    ProviderHealth health(ProviderConnectionExecution connection);

    default void close(ProviderConnectionId connectionId) {
        // Most stateless provider clients have no connection-scoped resources.
    }
}
