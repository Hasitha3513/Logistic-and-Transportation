package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;import java.time.*;import java.util.*;
public interface RouteDeviationEpisodeRepositoryPort {RouteDeviationEpisode save(RouteDeviationEpisode episode);Optional<RouteDeviationEpisode> find(UUID tenantId,UUID episodeId);Optional<RouteDeviationEpisode> findOpen(UUID tenantId,UUID vehicleId);List<RouteDeviationEpisode> history(UUID tenantId,UUID vehicleId,Instant from,Instant to,String cursor,int limit);}
