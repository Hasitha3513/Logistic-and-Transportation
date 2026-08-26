package com.transportlogistics.app.reporting.application.ports.in;

import com.transportlogistics.app.reporting.application.model.TripReportRecord;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface TripReportUseCase {

    Page<TripReportRecord> getTripReport(LocalDate fromDate,
                                         LocalDate toDate,
                                         int page,
                                         int limit,
                                         String status,
                                         UUID customerId);
}
