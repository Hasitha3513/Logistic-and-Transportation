package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.reporting.application.ports.in.DriverAssignmentUseCase;
import com.transportlogistics.app.reporting.application.ports.in.TripReportUseCase;
import com.transportlogistics.app.reporting.application.ports.in.VehicleUtilizationUseCase;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.DriverAssignmentResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.PageResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.TripReportResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.VehicleUtilizationResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.mappers.ReportingWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportingController {

    private final TripReportUseCase tripReports;
    private final DriverAssignmentUseCase driverAssignments;
    private final VehicleUtilizationUseCase vehicleUtilization;
    private final ReportingWebMapper mapper;

    @GetMapping("/dashboard/operations")
    public Map<String, Object> dashboard(@RequestParam(required = false) LocalDate date) {
        return Map.of("date", date == null ? LocalDate.now() : date, "status", "READY");
    }

    @GetMapping("/reports/trips")
    public PageResponse<TripReportResponse> trips(@RequestParam LocalDate fromDate,
                                                  @RequestParam LocalDate toDate,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int limit,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) UUID customerId) {
        var result = tripReports.getTripReport(fromDate, toDate, page, limit, status, customerId);
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/reports/driver-assignments")
    public List<DriverAssignmentResponse> driverAssignments(@RequestParam LocalDate fromDate,
                                                            @RequestParam LocalDate toDate,
                                                            @RequestParam(required = false) UUID driverId) {
        var records = driverAssignments.getDriverAssignmentReport(fromDate, toDate, driverId);
        return mapper.toDriverAssignmentResponseList(records);
    }

    @GetMapping("/reports/vehicle-utilization")
    public List<VehicleUtilizationResponse> vehicleUtilization(@RequestParam LocalDate fromDate,
                                                               @RequestParam LocalDate toDate,
                                                               @RequestParam(required = false) UUID vehicleId) {
        var records = vehicleUtilization.getVehicleUtilizationReport(fromDate, toDate, vehicleId);
        return mapper.toVehicleUtilizationResponseList(records);
    }
}
