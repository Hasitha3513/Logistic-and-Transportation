package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.reporting.application.model.DriverAssignmentReportRecord;
import com.transportlogistics.app.reporting.application.model.TripReportRecord;
import com.transportlogistics.app.reporting.application.model.VehicleUtilizationReportRecord;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.DriverAssignmentResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.TripReportResponse;
import com.transportlogistics.app.reporting.infrastructure.adapters.in.web.dto.response.VehicleUtilizationResponse;
import org.mapstruct.Mapper;

import com.transportlogistics.app.reporting.domain.model.OperationsDashboard;
import com.transportlogistics.app.reporting.web.dto.response.OperationsDashboardResponse;
import java.util.List;




@Mapper(componentModel = "spring")
public interface ReportingWebMapper {

    TripReportResponse toResponse(TripReportRecord record);

    List<TripReportResponse> toTripResponseList(List<TripReportRecord> records);

    DriverAssignmentResponse toResponse(DriverAssignmentReportRecord record);

    List<DriverAssignmentResponse> toDriverAssignmentResponseList(List<DriverAssignmentReportRecord> records);

    VehicleUtilizationResponse toResponse(VehicleUtilizationReportRecord record);
    OperationsDashboardResponse toResponse(OperationsDashboard domain);
    List<VehicleUtilizationResponse> toVehicleUtilizationResponseList(List<VehicleUtilizationReportRecord> records);
}
