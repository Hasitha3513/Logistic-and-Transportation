package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.reporting.application.ports.in.DriverAssignmentUseCase;
import com.transportlogistics.app.reporting.application.ports.in.OperationsDashboardUseCase;
import com.transportlogistics.app.reporting.application.ports.in.TripReportUseCase;
import com.transportlogistics.app.reporting.application.ports.in.VehicleUtilizationUseCase;
import com.transportlogistics.app.reporting.application.ports.in.FreightReportUseCase;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.FreightReportResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.DriverAssignmentResponse;
import com.transportlogistics.app.reporting.web.dto.response.OperationsDashboardResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.PageResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.TripReportResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.VehicleUtilizationResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.mappers.ReportingWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportingController {

    private final OperationsDashboardUseCase operationsDashboard;
    private final TripReportUseCase tripReports;
    private final DriverAssignmentUseCase driverAssignments;
    private final VehicleUtilizationUseCase vehicleUtilization;
    private final FreightReportUseCase freightReports;
    private final ReportingWebMapper mapper;

    @GetMapping("/dashboard/operations")
    public OperationsDashboardResponse dashboard(@RequestParam(required = false) LocalDate date) {
        var domain = operationsDashboard.getOperationsDashboard(date);
        return mapper.toResponse(domain);
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

    @GetMapping("/reports/freight/summary")
    public FreightReportResponse.Summary freightSummary(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
            @RequestParam(required=false) UUID customerId, @RequestParam(required=false) UUID freightOrderId,
            @RequestParam(required=false) UUID originLocationId, @RequestParam(required=false) UUID destinationLocationId,
            @RequestParam(required=false) String loadPlanStatus, @RequestParam(required=false) String exceptionStatus,
            @RequestParam(required=false) String exceptionType, @RequestParam(required=false) String policyStatus,
            @RequestParam(required=false) String claimStatus) {
        return FreightReportResponse.summary(freightReports.summary(filter(fromDate,toDate,customerId,freightOrderId,
                originLocationId,destinationLocationId,loadPlanStatus,exceptionStatus,exceptionType,policyStatus,claimStatus)));
    }

    @GetMapping("/reports/freight/shipments")
    public PageResponse<FreightReportResponse.Shipment> freightShipments(
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="createdAt") String sort,
            @RequestParam(defaultValue="DESC") String direction, @RequestParam(required=false) UUID customerId,
            @RequestParam(required=false) UUID freightOrderId, @RequestParam(required=false) UUID originLocationId,
            @RequestParam(required=false) UUID destinationLocationId, @RequestParam(required=false) String loadPlanStatus,
            @RequestParam(required=false) String exceptionStatus, @RequestParam(required=false) String exceptionType,
            @RequestParam(required=false) String policyStatus, @RequestParam(required=false) String claimStatus) {
        var result=freightReports.shipments(filter(fromDate,toDate,customerId,freightOrderId,originLocationId,destinationLocationId,
                loadPlanStatus,exceptionStatus,exceptionType,policyStatus,claimStatus),page,size,sort,direction);
        return new PageResponse<>(result.getContent().stream().map(FreightReportResponse::shipment).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping(value="/reports/freight/export", produces="text/csv")
    public ResponseEntity<byte[]> freightExport(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
            @RequestParam(required=false) UUID customerId, @RequestParam(required=false) UUID freightOrderId,
            @RequestParam(required=false) UUID originLocationId, @RequestParam(required=false) UUID destinationLocationId,
            @RequestParam(required=false) String loadPlanStatus, @RequestParam(required=false) String exceptionStatus,
            @RequestParam(required=false) String exceptionType, @RequestParam(required=false) String policyStatus,
            @RequestParam(required=false) String claimStatus) {
        byte[] body=freightReports.exportCsv(filter(fromDate,toDate,customerId,freightOrderId,originLocationId,destinationLocationId,
                loadPlanStatus,exceptionStatus,exceptionType,policyStatus,claimStatus));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=freight-report.csv").body(body);
    }

    private FreightReportUseCase.Filter filter(LocalDate from,LocalDate to,UUID customer,UUID order,UUID origin,UUID destination,
            String loadStatus,String exceptionStatus,String exceptionType,String policyStatus,String claimStatus) {
        return new FreightReportUseCase.Filter(from,to,customer,order,origin,destination,loadStatus,exceptionStatus,exceptionType,policyStatus,claimStatus);
    }
}
