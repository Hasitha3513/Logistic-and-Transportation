package com.transportlogistics.app.reporting.application.ports.out;

import com.transportlogistics.app.trip.DriverAssignmentReportItem;
import com.transportlogistics.app.trip.TripReportItem;
import com.transportlogistics.app.trip.VehicleTripReportItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TripReportReadPort {

    Page<TripReportItem> findTripReports(OffsetDateTime from, OffsetDateTime to, String status, UUID customerId, Pageable pageable);

    List<DriverAssignmentReportItem> findDriverAssignments(OffsetDateTime from, OffsetDateTime to, UUID driverId);

    List<VehicleTripReportItem> findVehicleTrips(OffsetDateTime from, OffsetDateTime to, UUID vehicleId);

    List<TripReportItem> findAllTrips();
}
