package com.transportlogistics.app.integration;
import java.util.*;
/** Published read-only lookup for the frozen US-46 export family. */
public interface PayrollIntegrationConfigurationLookup { Optional<UUID> activePayrollConfiguration(UUID tenantId); }
