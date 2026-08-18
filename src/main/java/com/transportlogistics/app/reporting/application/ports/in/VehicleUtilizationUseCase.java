package com.transportlogistics.app.reporting.application.ports.in;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import com.transportlogistics.app.reporting.web.dto.response.reporting.VehicleUtilizationDto;

public interface VehicleUtilizationUseCase {
    Page<VehicleUtilizationDto> getVehicleUtilizationReport(LocalDate fromDate,
                                                              LocalDate toDate,
                                                              int page,
                                                              int limit,
                                                              UUID vehicleId);
}
