package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CargoExceptionJpaRepository extends JpaRepository<CargoExceptionEntity, UUID> {

    @Query("""
            SELECT e FROM CargoExceptionEntity e
            WHERE (:freightOrderId IS NULL OR e.freightOrderId = :freightOrderId)
              AND (:manifestId IS NULL     OR e.manifestId     = :manifestId)
              AND (:type       IS NULL     OR e.exceptionType  = :type)
              AND (:status     IS NULL     OR e.status         = :status)
            ORDER BY e.createdAt DESC
            """)
    List<CargoExceptionEntity> findFiltered(
            @Param("freightOrderId") UUID freightOrderId,
            @Param("manifestId")     UUID manifestId,
            @Param("type")           String type,
            @Param("status")         String status,
            Pageable pageable
    );
}
