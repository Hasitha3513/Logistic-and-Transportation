package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

interface TripJpaRepository extends JpaRepository<TripEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TripEntity t where t.id = :id")
    java.util.Optional<TripEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select count(t) from TripEntity t
            where t.vehicleId = :vehicleId
              and t.status not in ('CANCELLED', 'COMPLETED', 'CLOSED', 'REJECTED')
              and t.requestedStartTime < :to
              and t.requestedEndTime > :from
            """)
    long countOverlaps(@Param("vehicleId") UUID vehicleId, @Param("from") OffsetDateTime from,
                       @Param("to") OffsetDateTime to);

    @Query("""
            select count(t) from TripEntity t
            where t.vehicleId = :vehicleId
              and t.id <> :excludeTripId
              and t.status not in ('CANCELLED', 'COMPLETED', 'CLOSED', 'REJECTED')
              and t.requestedStartTime < :to
              and t.requestedEndTime > :from
            """)
    long countOverlapsExcluding(@Param("vehicleId") UUID vehicleId, @Param("from") OffsetDateTime from,
                                @Param("to") OffsetDateTime to, @Param("excludeTripId") UUID excludeTripId);

    @Query("""
            select count(t) from TripEntity t
            where t.driverId = :driverId
              and t.status not in ('CANCELLED', 'COMPLETED', 'CLOSED', 'REJECTED')
              and t.requestedStartTime < :to
              and t.requestedEndTime > :from
            """)
    long countDriverOverlaps(@Param("driverId") UUID driverId, @Param("from") OffsetDateTime from,
                             @Param("to") OffsetDateTime to);

    @Query("""
            select count(t) from TripEntity t
            where t.driverId = :driverId
              and t.id <> :excludeTripId
              and t.status not in ('CANCELLED', 'COMPLETED', 'CLOSED', 'REJECTED')
              and t.requestedStartTime < :to
              and t.requestedEndTime > :from
            """)
    long countDriverOverlapsExcluding(@Param("driverId") UUID driverId, @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to,
                                      @Param("excludeTripId") UUID excludeTripId);

    @Query("""
            select new com.transportlogistics.app.trip.TripReportItem(
                t.id, t.tripNumber, t.status, t.customerId, t.vehicleId, t.driverId, t.routeId,
                t.requestedStartTime, t.requestedEndTime, t.actualStartTime, t.actualEndTime,
                t.startOdometerKm, t.endOdometerKm, t.completionRemarks, t.createdAt
            )
            from TripEntity t
            where t.requestedStartTime >= :from and t.requestedStartTime < :to
              and (:status is null or t.status = :status)
              and (:customerId is null or t.customerId = :customerId)
            order by t.requestedStartTime asc
            """)
    org.springframework.data.domain.Page<com.transportlogistics.app.trip.TripReportItem> findTripReportItems(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("status") String status,
            @Param("customerId") UUID customerId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            select new com.transportlogistics.app.trip.DriverAssignmentReportItem(
                t.id, t.tripNumber, t.status, t.driverId, t.vehicleId, t.routeId,
                t.requestedStartTime, t.requestedEndTime, t.actualStartTime, t.actualEndTime,
                t.createdAt
            )
            from TripEntity t
            where t.driverId is not null
              and t.requestedStartTime >= :from and t.requestedStartTime < :to
              and (:driverId is null or t.driverId = :driverId)
            order by t.requestedStartTime asc
            """)
    java.util.List<com.transportlogistics.app.trip.DriverAssignmentReportItem> findDriverAssignmentItems(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("driverId") UUID driverId);

    @Query("""
            select new com.transportlogistics.app.trip.VehicleTripReportItem(
                t.id, t.tripNumber, t.status, t.vehicleId,
                t.requestedStartTime, t.requestedEndTime, t.actualStartTime, t.actualEndTime,
                t.startOdometerKm, t.endOdometerKm
            )
            from TripEntity t
            where t.vehicleId is not null
              and t.requestedStartTime >= :from and t.requestedStartTime < :to
              and (:vehicleId is null or t.vehicleId = :vehicleId)
            order by t.requestedStartTime asc
            """)
    java.util.List<com.transportlogistics.app.trip.VehicleTripReportItem> findVehicleTripItems(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("vehicleId") UUID vehicleId);

    @Query("""
            select new com.transportlogistics.app.trip.TripReportItem(
                t.id, t.tripNumber, t.status, t.customerId, t.vehicleId, t.driverId, t.routeId,
                t.requestedStartTime, t.requestedEndTime, t.actualStartTime, t.actualEndTime,
                t.startOdometerKm, t.endOdometerKm, t.completionRemarks, t.createdAt
            )
            from TripEntity t
            order by t.requestedStartTime desc
            """)
    java.util.List<com.transportlogistics.app.trip.TripReportItem> findAllTripReportItems();

    @Query("""
            select t from TripEntity t
            where t.routeId = :routeId
              and (:from is null or coalesce(t.actualStartTime, t.requestedStartTime) >= :from)
              and (:to is null or coalesce(t.actualEndTime, t.requestedEndTime, t.actualStartTime, t.requestedStartTime) <= :to)
            order by coalesce(t.actualStartTime, t.requestedStartTime) desc
            """)
    java.util.List<TripEntity> findByRouteIdAndDateRange(
            @Param("routeId") UUID routeId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
