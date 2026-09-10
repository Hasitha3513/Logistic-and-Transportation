package com.transportlogistics.app.tracking.adapters.configuration;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.tracking.application.TrackingService;
import com.transportlogistics.app.tracking.application.TrackingMaintenanceService;
import com.transportlogistics.app.tracking.application.TrackingProviderManagementService;
import com.transportlogistics.app.tracking.application.GeofenceEvaluationService;
import com.transportlogistics.app.tracking.application.GeofenceManagementService;
import com.transportlogistics.app.tracking.adapters.inbound.security.SecuredGeofenceUseCases;
import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.application.provider.TrackingProviderAdapterRegistry;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderAdapter;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderConnectionStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceLocationLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementSupportPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionPublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.*;
@Configuration class TrackingConfiguration {
 @Bean TrackingService trackingUseCase(TrackingStore store,FleetReportingQuery fleet,Clock clock){return new TrackingService(store,fleet,clock);}
 @Bean TrackingMaintenanceService trackingMaintenanceUseCase(TrackingStore store,Clock clock){return new TrackingMaintenanceService(store,clock);}
 @Bean TrackingProviderAdapterRegistry trackingProviderAdapterRegistry(List<TrackingProviderAdapter> adapters){return new TrackingProviderAdapterRegistry(adapters);}
 @Bean TrackingProviderManagementService trackingProviderManagementUseCase(TrackingProviderConnectionStore connections,TrackingDeviceProviderBindingStore bindings,TrackingProviderAdapterRegistry adapters,IntegrationSecretResolver secrets,Clock clock){return new TrackingProviderManagementService(connections,bindings,adapters,secrets,clock);}
 @Bean GeofenceTransitionPublisherPort geofenceTransitionPublisherPort(){return event->{ };}
 @Bean GeofenceEvaluationService geofenceEvaluationUseCase(GeofenceRepositoryPort geofences,VehicleGeofenceStateRepositoryPort states,GeofenceTransitionRepositoryPort transitions,GeofenceTransitionPublisherPort publisher,GeofenceEvaluationTransactionPort transaction){return new GeofenceEvaluationService(geofences,states,transitions,publisher,transaction);}
 @Bean GeofenceManagementService geofenceManagementService(GeofenceRepositoryPort geofences,VehicleGeofenceStateRepositoryPort states,GeofenceTransitionRepositoryPort transitions,GeofenceLocationLookupPort locations,GeofenceManagementSupportPort support,GeofenceManagementTransactionPort transaction){return new GeofenceManagementService(geofences,states,transitions,locations,support,transaction);}
 @Bean @Primary SecuredGeofenceUseCases securedGeofenceUseCases(GeofenceManagementService service){return new SecuredGeofenceUseCases(service,service);}
}
