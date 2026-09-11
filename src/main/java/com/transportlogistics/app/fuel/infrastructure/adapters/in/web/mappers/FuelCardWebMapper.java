package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.domain.model.FuelCard;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelCardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface FuelCardWebMapper {
    @Mapping(target="status", expression="java(card.status().name())")
    @Mapping(target="providerSyncStatus", constant="NOT_CONFIGURED")
    FuelCardResponse toResponse(FuelCard card);
}
