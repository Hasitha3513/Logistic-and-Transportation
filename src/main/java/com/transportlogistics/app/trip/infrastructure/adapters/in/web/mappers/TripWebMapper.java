package com.transportlogistics.app.trip.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripHistoryResponse;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripOperationalEventResponse;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TripWebMapper {

    TripResponse toResponse(Trip trip);

    List<TripResponse> toTripResponseList(List<Trip> trips);

    TripHistoryResponse toResponse(TripHistoryEntry entry);

    List<TripHistoryResponse> toTripHistoryResponseList(List<TripHistoryEntry> entries);

    TripOperationalEventResponse toResponse(TripOperationalEvent event);

    List<TripOperationalEventResponse> toTripOperationalEventResponseList(List<TripOperationalEvent> events);
}
