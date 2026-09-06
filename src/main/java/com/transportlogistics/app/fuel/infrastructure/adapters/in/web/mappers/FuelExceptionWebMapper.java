package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelExceptionResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel="spring")
public interface FuelExceptionWebMapper {
    FuelExceptionResponse toResponse(FuelExceptionCase value);
    default FuelExceptionResponse.Detail toResponse(FuelExceptionUseCase.Detail value){return new FuelExceptionResponse.Detail(toResponse(value.value()),value.evidence(),value.notes(),value.corrections(),value.history());}
}
