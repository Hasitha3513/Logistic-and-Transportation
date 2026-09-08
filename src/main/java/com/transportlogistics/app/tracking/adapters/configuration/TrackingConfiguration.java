package com.transportlogistics.app.tracking.adapters.configuration;
import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.tracking.application.TrackingService;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration class TrackingConfiguration {@Bean TrackingService trackingUseCase(TrackingStore store,FleetReportingQuery fleet,Clock clock){return new TrackingService(store,fleet,clock);}}
