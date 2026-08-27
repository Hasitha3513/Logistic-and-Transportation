package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

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
class TripReportingAdapter implements TripReportingQuery {

    private final TripJpaRepository repository;

    @Override
    public Page<TripReportItem> findTripReports(OffsetDateTime from, OffsetDateTime to, String status, UUID customerId, Pageable pageable) {
        return repository.findTripReportItems(from, to, status, customerId, pageable);
    }

    @Override
    public List<DriverAssignmentReportItem> findDriverAssignments(OffsetDateTime from, OffsetDateTime to, UUID driverId) {
        return repository.findDriverAssignmentItems(from, to, driverId);
    }

    @Override
    public List<VehicleTripReportItem> findVehicleTrips(OffsetDateTime from, OffsetDateTime to, UUID vehicleId) {
        return repository.findVehicleTripItems(from, to, vehicleId);
    }

    @Override
    public List<TripReportItem> findAllTripSummaries() {
        return repository.findAllTripReportItems();
    }
}
