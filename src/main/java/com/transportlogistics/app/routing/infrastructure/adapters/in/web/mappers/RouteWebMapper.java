package com.transportlogistics.app.routing.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RouteWebMapper {

    RouteResponse toResponse(Route route);

    List<RouteResponse> toResponseList(List<Route> routes);
}
