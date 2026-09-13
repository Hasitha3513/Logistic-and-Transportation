package com.transportlogistics.app.tracking.ports.inbound;
import com.transportlogistics.app.tracking.domain.routedeviation.*;import java.time.*;import java.util.*;
public interface RouteDeviationQueryUseCase {List<RouteDeviationRule> rules(UUID tenantId,int offset,int size);List<VehicleRouteDeviationState> states(UUID tenantId,int offset,int size);List<RouteDeviationEpisode> episodes(UUID tenantId,Instant from,Instant to,String cursor,int limit);List<RouteDeviationReview> reviews(UUID tenantId,UUID episodeId,int limit);}
