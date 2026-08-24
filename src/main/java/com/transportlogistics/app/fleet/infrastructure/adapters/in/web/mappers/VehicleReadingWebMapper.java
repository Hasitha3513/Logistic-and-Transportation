package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fleet.VehicleMileageSummary;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleReadingWebMapper {

    default VehicleReadingResponse toResponse(VehicleReading reading) {
        return VehicleReadingResponse.from(reading);
    }

    default List<VehicleReadingResponse> toReadingResponseList(List<VehicleReading> readings) {
        if (readings == null) return List.of();
        return readings.stream().map(VehicleReadingResponse::from).toList();
    }

    default VehicleMeterResetResponse toResponse(VehicleMeterReset reset) {
        return VehicleMeterResetResponse.from(reset);
    }

    default VehicleMileageSummaryResponse toResponse(VehicleMileageSummary summary) {
        return VehicleMileageSummaryResponse.from(summary);
    }

    default TripDistanceSummaryResponse toResponse(TripDistanceSummary summary) {
        return TripDistanceSummaryResponse.from(summary);
    }

    default LatestVehicleReadingsResponse toResponse(VehicleReadingUseCase.LatestReadings latest) {
        if (latest == null) return null;
        return new LatestVehicleReadingsResponse(
                latest.vehicleId(),
                latest.odometer().map(LatestVehicleReadingsResponse.ReadingSnapshot::from).orElse(null),
                latest.engineHours().map(LatestVehicleReadingsResponse.ReadingSnapshot::from).orElse(null)
        );
    }
}
