package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.FuelPerformanceQuery;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelPerformanceResponses;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelPerformanceWebMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/v1/fuel/performance")
public class FuelPerformanceController {
    private final FuelPerformanceQuery query;
    private final FuelPerformanceWebMapper mapper;

    public FuelPerformanceController(FuelPerformanceQuery query, FuelPerformanceWebMapper mapper) {
        this.query = query;
        this.mapper = mapper;
    }

    @GetMapping("/summary")
    public FuelPerformanceResponses.Summary summary(Parameters parameters) {
        return mapper.toResponse(query.summary(parameters.criteria()));
    }

    @GetMapping("/vehicles")
    public FuelPerformanceResponses.Page<FuelPerformanceResponses.Vehicle> vehicles(
            Parameters parameters, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "adverseVariancePercent") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return mapper.toVehiclePage(query.vehicles(parameters.criteria(), page, size, sort, direction));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public FuelPerformanceResponses.Vehicle vehicle(@PathVariable UUID vehicleId, Parameters parameters) {
        return mapper.toResponse(query.vehicle(vehicleId, parameters.criteria()));
    }

    @GetMapping("/drivers")
    public FuelPerformanceResponses.Page<FuelPerformanceResponses.Driver> drivers(
            Parameters parameters, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "adverseVariancePercent") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return mapper.toDriverPage(query.drivers(parameters.criteria(), page, size, sort, direction));
    }

    @GetMapping("/drivers/{driverId}")
    public FuelPerformanceResponses.Driver driver(@PathVariable UUID driverId, Parameters parameters) {
        return mapper.toResponse(query.driver(driverId, parameters.criteria()));
    }

    @GetMapping("/trends")
    public List<FuelPerformanceResponses.Trend> trends(Parameters parameters) {
        return mapper.toTrendResponses(query.trends(parameters.criteria()));
    }

    public record Parameters(Integer preset, LocalDate from, LocalDate to, UUID vehicleId, UUID driverId,
                             UUID vehicleTypeId, String fuelType, String measurementMode) {
        FuelPerformanceQuery.Criteria criteria() {
            return new FuelPerformanceQuery.Criteria(preset, from, to, vehicleId, driverId, vehicleTypeId, fuelType,
                    mode(measurementMode));
        }

        private static FuelPerformanceQuery.MeasurementMode mode(String value) {
            if (value == null || value.isBlank()) return FuelPerformanceQuery.MeasurementMode.DISTANCE;
            try {
                return FuelPerformanceQuery.MeasurementMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException("FUEL_PERFORMANCE_UNSUPPORTED_MEASUREMENT",
                        "Unsupported fuel performance measurement mode");
            }
        }
    }
}
