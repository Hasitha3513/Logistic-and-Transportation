package com.transportlogistics.app.trip;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public read-only query contract published by the Trip module.
 */
public interface TripReportingQuery {

    Page<TripReportItem> findTripReports(OffsetDateTime from, OffsetDateTime to, String status, UUID customerId, Pageable pageable);

    List<DriverAssignmentReportItem> findDriverAssignments(OffsetDateTime from, OffsetDateTime to, UUID driverId);

    List<VehicleTripReportItem> findVehicleTrips(OffsetDateTime from, OffsetDateTime to, UUID vehicleId);
}
