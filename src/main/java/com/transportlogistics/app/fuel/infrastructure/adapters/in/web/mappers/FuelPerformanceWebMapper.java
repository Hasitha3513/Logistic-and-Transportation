package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelPerformanceResponses;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuelPerformanceWebMapper {
    FuelPerformanceResponses.Period toResponse(FuelPerformanceQuery.Period value);
    FuelPerformanceResponses.Baseline toResponse(FuelPerformanceQuery.Baseline value);
    FuelPerformanceResponses.Metrics toResponse(FuelPerformanceQuery.Metrics value);
    FuelPerformanceResponses.Summary toResponse(FuelPerformanceQuery.FuelPerformanceSummary value);
    FuelPerformanceResponses.Vehicle toResponse(FuelPerformanceQuery.VehicleFuelPerformance value);
    FuelPerformanceResponses.Driver toResponse(FuelPerformanceQuery.DriverFuelPerformance value);
    FuelPerformanceResponses.Trend toResponse(FuelPerformanceQuery.FuelPerformanceTrend value);
    List<FuelPerformanceResponses.Trend> toTrendResponses(List<FuelPerformanceQuery.FuelPerformanceTrend> values);

    default FuelPerformanceResponses.Page<FuelPerformanceResponses.Vehicle> toVehiclePage(
            FuelPerformanceQuery.Page<FuelPerformanceQuery.VehicleFuelPerformance> value) {
        return new FuelPerformanceResponses.Page<>(value.content().stream().map(this::toResponse).toList(),
                value.page(), value.size(), value.totalElements(), value.totalPages());
    }

    default FuelPerformanceResponses.Page<FuelPerformanceResponses.Driver> toDriverPage(
            FuelPerformanceQuery.Page<FuelPerformanceQuery.DriverFuelPerformance> value) {
        return new FuelPerformanceResponses.Page<>(value.content().stream().map(this::toResponse).toList(),
                value.page(), value.size(), value.totalElements(), value.totalPages());
    }
}
