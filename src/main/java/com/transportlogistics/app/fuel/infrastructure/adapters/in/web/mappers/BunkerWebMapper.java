package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.domain.model.BunkerStockMovement;
import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.DipReading;
import com.transportlogistics.app.fuel.domain.model.StockAdjustment;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.BunkerTankCreateRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.BunkerTankUpdateRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.BunkerTransferRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BunkerWebMapper {

    BunkerTankUseCase.CreateTankCommand toCommand(BunkerTankCreateRequest request);

    BunkerTankUseCase.UpdateTankCommand toCommand(BunkerTankUpdateRequest request);

    BunkerTankUseCase.TransferCommand toCommand(BunkerTransferRequest request);

    @Mapping(target = "availableCapacityLiters", expression = "java(tank.availableCapacity())")
    @Mapping(target = "lowStock", expression = "java(tank.isLowStock())")
    BunkerTankResponse toResponse(BunkerTank tank);

    BunkerStockMovementResponse toResponse(BunkerStockMovement movement);

    DipReadingResponse toResponse(DipReading reading);

    StockAdjustmentResponse toResponse(StockAdjustment adjustment);
}
