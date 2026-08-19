package com.transportlogistics.app.reporting.infrastructure.adapters.out.trip;

import com.transportlogistics.app.reporting.application.ports.out.TripReportReadPort;
import com.transportlogistics.app.trip.DriverAssignmentReportItem;
import com.transportlogistics.app.trip.TripReportItem;
import com.transportlogistics.app.trip.TripReportingQuery;
import com.transportlogistics.app.trip.VehicleTripReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TripReportReadAdapter implements TripReportReadPort {

    private final TripReportingQuery query;

    @Override
    public Page<TripReportItem> findTripReports(OffsetDateTime from, OffsetDateTime to, String status, UUID customerId, Pageable pageable) {
        return query.findTripReports(from, to, status, customerId, pageable);
    }

    @Override
    public List<DriverAssignmentReportItem> findDriverAssignments(OffsetDateTime from, OffsetDateTime to, UUID driverId) {
        return query.findDriverAssignments(from, to, driverId);
    }

    @Override
    public List<VehicleTripReportItem> findVehicleTrips(OffsetDateTime from, OffsetDateTime to, UUID vehicleId) {
        return query.findVehicleTrips(from, to, vehicleId);
    }
}
