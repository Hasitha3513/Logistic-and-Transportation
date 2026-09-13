package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.*;
public interface RouteDeviationEventPublisherPort {void publish(VehicleRouteDeviationDetectedV1 event);void publish(VehicleRouteDeviationEscalatedV1 event);}
