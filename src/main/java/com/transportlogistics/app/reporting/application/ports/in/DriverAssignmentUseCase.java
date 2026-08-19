package com.transportlogistics.app.reporting.application.ports.in;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import com.transportlogistics.app.reporting.web.dto.response.reporting.DriverAssignmentDto;

public interface DriverAssignmentUseCase {
    Page<DriverAssignmentDto> getDriverAssignmentReport(LocalDate fromDate,
                                                         LocalDate toDate,
                                                         int page,
                                                         int limit,
                                                         UUID driverId);
}
