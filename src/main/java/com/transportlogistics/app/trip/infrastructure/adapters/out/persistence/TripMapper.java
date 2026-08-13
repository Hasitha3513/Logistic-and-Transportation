package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.domain.model.Trip;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface TripMapper {
    Trip toDomain(TripEntity entity);

    TripEntity toEntity(Trip domain);
}