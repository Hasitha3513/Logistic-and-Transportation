package com.transportlogistics.app.reporting.application.ports.in;

import com.transportlogistics.app.reporting.application.model.VehicleUtilizationReportRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VehicleUtilizationUseCase {

    List<VehicleUtilizationReportRecord> getVehicleUtilizationReport(LocalDate fromDate,
                                                                     LocalDate toDate,
                                                                     UUID vehicleId);
}
