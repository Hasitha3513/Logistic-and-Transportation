package com.transportlogistics.app.reporting.infrastructure.adapters.in.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TripReportRepository extends JpaRepository<com.transportlogistics.app.trip.infrastructure.adapters.out.persistence.TripEntity, UUID> {
    @Query("SELECT t.id as tripId, t.tripNumber as tripNumber, t.status as status, t.vehicleId as vehicleId, t.driverId as driverId, t.routeId as routeId, t.customerId as customerId, FUNCTION('DATE', t.requestedStartTime) as requestedStartTime, FUNCTION('DATE', t.requestedEndTime) as requestedEndTime, FUNCTION('DATE', t.actualStartTime) as actualStartTime, FUNCTION('DATE', t.actualEndTime) as actualEndTime FROM TripEntity t WHERE FUNCTION('DATE', t.requestedStartTime) BETWEEN :fromDate AND :toDate AND (:status IS NULL OR t.status = :status) AND (:customerId IS NULL OR t.customerId = :customerId)")
    Page<TripReportProjection> findTripReport(LocalDate fromDate, LocalDate toDate, String status, UUID customerId, Pageable pageable);
}
