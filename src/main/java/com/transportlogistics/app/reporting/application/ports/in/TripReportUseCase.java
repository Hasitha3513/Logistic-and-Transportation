package com.transportlogistics.app.reporting.application.ports.in;

import java.time.LocalDate;
import java.util.UUID;
import com.transportlogistics.app.reporting.web.dto.response.reporting.TripReportDto;
import org.springframework.data.domain.Page;

public interface TripReportUseCase {
    Page<TripReportDto> getTripReport(LocalDate fromDate,
                                      LocalDate toDate,
                                      int page,
                                      int limit,
                                      String status,
                                      UUID customerId);
}
