package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface MaintenanceScheduleJpaRepository extends JpaRepository<MaintenanceScheduleEntity, UUID> {

    List<MaintenanceScheduleEntity> findByVehicleIdOrderByScheduledStartAsc(UUID vehicleId);

    @Query("""
            select m from MaintenanceScheduleEntity m
            where m.status = 'SCHEDULED'
              and m.scheduledStart > :from
              and m.scheduledStart <= :to
            order by m.scheduledStart
            """)
    List<MaintenanceScheduleEntity> findScheduledStartingBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            select count(m) > 0 from MaintenanceScheduleEntity m
            where m.vehicleId = :vehicleId
              and m.status in :statuses
              and m.scheduledStart < :to
              and m.scheduledEnd > :from
            """)
    boolean hasOverlap(
            @Param("vehicleId") UUID vehicleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("statuses") List<String> statuses
    );

    @Query("""
            select count(m) > 0 from MaintenanceScheduleEntity m
            where m.vehicleId = :vehicleId
              and m.id <> :excludeScheduleId
              and m.status in :statuses
              and m.scheduledStart < :to
              and m.scheduledEnd > :from
            """)
    boolean hasOverlapExcluding(
            @Param("vehicleId") UUID vehicleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("statuses") List<String> statuses,
            @Param("excludeScheduleId") UUID excludeScheduleId
    );
}
