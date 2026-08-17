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
}
