package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.TripFuelCost;
import com.transportlogistics.app.fuel.TripFuelCostLine;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.TripFuelCostLineResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.TripFuelCostResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TripFuelCostWebMapper {

    TripFuelCostLineResponse toResponse(TripFuelCostLine line);

    List<TripFuelCostLineResponse> toLineResponseList(List<TripFuelCostLine> lines);

    default TripFuelCostResponse toResponse(TripFuelCost cost) {
        if (cost == null) return null;
        var lines = toLineResponseList(cost.lines());
        return new TripFuelCostResponse(
                cost.tripId(),
                cost.vehicleId(),
                cost.totalFuelQuantityLiters(),
                cost.currencyCode(),
                cost.totalFuelCost(),
                cost.tripDistanceKm(),
                cost.costPerKm(),
                cost.litersPer100Km(),
                cost.fuelIssueCount(),
                cost.unpricedIssueCount(),
                cost.distanceStatus(),
                cost.calculationStatus(),
                lines,
                cost.calculatedAt()
        );
    }
}
