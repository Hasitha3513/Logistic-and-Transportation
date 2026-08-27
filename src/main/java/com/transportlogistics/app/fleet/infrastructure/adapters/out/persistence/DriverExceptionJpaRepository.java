package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface DriverExceptionJpaRepository extends JpaRepository<DriverExceptionEntity, UUID> {

    List<DriverExceptionEntity> findByDriverIdOrderByStartTimeAsc(UUID driverId);

    @Query("""
            select count(e) > 0 from DriverExceptionEntity e
            where e.driverId = :driverId
              and e.status in :statuses
              and e.startTime < :to
              and e.endTime > :from
            """)
    boolean hasOverlap(
            @Param("driverId") UUID driverId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("statuses") List<String> statuses
    );

    @Query("""
            select count(e) > 0 from DriverExceptionEntity e
            where e.driverId = :driverId
              and e.id <> :excludeExceptionId
              and e.status in :statuses
              and e.startTime < :to
              and e.endTime > :from
            """)
    boolean hasOverlapExcluding(
            @Param("driverId") UUID driverId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("statuses") List<String> statuses,
            @Param("excludeExceptionId") UUID excludeExceptionId
    );
}
