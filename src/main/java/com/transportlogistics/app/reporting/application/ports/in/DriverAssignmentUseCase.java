package com.transportlogistics.app.reporting.application.ports.in;

import com.transportlogistics.app.reporting.application.model.DriverAssignmentReportRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverAssignmentUseCase {

    List<DriverAssignmentReportRecord> getDriverAssignmentReport(LocalDate fromDate,
                                                                 LocalDate toDate,
                                                                 UUID driverId);
}
