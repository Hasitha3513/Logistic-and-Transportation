package com.transportlogistics.app.tracking.adapters.configuration;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.tracking.application.TrackingService;
import com.transportlogistics.app.tracking.application.TrackingMaintenanceService;
import com.transportlogistics.app.tracking.application.TrackingProviderManagementService;
import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.*;
@Configuration class TrackingConfiguration {
 @Bean TrackingService trackingUseCase(TrackingStore store,FleetReportingQuery fleet,Clock clock){return new TrackingService(store,fleet,clock);}
 @Bean TrackingMaintenanceService trackingMaintenanceUseCase(TrackingStore store,Clock clock){return new TrackingMaintenanceService(store,clock);}
 @Bean TrackingProviderAdapterRegistry trackingProviderAdapterRegistry(List<TrackingProviderAdapter> adapters){return new TrackingProviderAdapterRegistry(adapters);}
 @Bean TrackingProviderManagementService trackingProviderManagementUseCase(TrackingProviderConnectionStore connections,TrackingDeviceProviderBindingStore bindings,TrackingProviderAdapterRegistry adapters,IntegrationSecretResolver secrets,Clock clock){return new TrackingProviderManagementService(connections,bindings,adapters,secrets,clock);}
}
