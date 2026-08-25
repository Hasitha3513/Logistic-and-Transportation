package com.transportlogistics.app.routing.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.domain.model.RouteDisruption;
import com.transportlogistics.app.routing.domain.model.RouteOptimizationResult;
import com.transportlogistics.app.routing.domain.model.RoutePerformanceAnalytics;
import com.transportlogistics.app.routing.domain.model.RouteRevision;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteDisruptionResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteOptimizationResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RoutePerformanceResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteRevisionResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RouteWebMapper {

    RouteResponse toResponse(Route route);

    List<RouteResponse> toResponseList(List<Route> routes);

    RouteRevisionResponse toRevisionResponse(RouteRevision revision);

    List<RouteRevisionResponse> toRevisionResponseList(List<RouteRevision> revisions);

    RouteDisruptionResponse toDisruptionResponse(RouteDisruption disruption);

    List<RouteDisruptionResponse> toDisruptionResponseList(List<RouteDisruption> disruptions);

    RouteOptimizationResponse toOptimizationResponse(RouteOptimizationResult optimizationResult);

    RoutePerformanceResponse toPerformanceResponse(RoutePerformanceAnalytics performanceAnalytics);
}
