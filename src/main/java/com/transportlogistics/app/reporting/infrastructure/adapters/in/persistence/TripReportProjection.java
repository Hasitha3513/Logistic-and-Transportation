package com.transportlogistics.app.reporting.infrastructure.adapters.in.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

public interface TripReportProjection {
    UUID getTripId();
    String getTripNumber();
    String getStatus();
    LocalDate getRequestedStartTime();
    LocalDate getRequestedEndTime();
    LocalDate getActualStartTime();
    LocalDate getActualEndTime();
    UUID getVehicleId();
    UUID getDriverId();
    UUID getRouteId();
    UUID getCustomerId();
}
