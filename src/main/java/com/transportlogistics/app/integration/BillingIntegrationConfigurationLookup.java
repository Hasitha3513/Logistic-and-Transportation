package com.transportlogistics.app.integration;

import java.util.Optional;
import java.util.UUID;
public interface BillingIntegrationConfigurationLookup { Optional<UUID> activeBillingConfiguration(UUID tenantId); }
