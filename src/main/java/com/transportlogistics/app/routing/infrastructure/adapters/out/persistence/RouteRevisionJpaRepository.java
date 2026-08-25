package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RouteRevisionJpaRepository extends JpaRepository<RouteRevisionEntity, UUID> {
    List<RouteRevisionEntity> findByRouteIdOrderByRevisionNumberDesc(UUID routeId);

    Optional<RouteRevisionEntity> findByRouteIdAndRevisionNumber(UUID routeId, int revisionNumber);

    @Query("SELECT COALESCE(MAX(r.revisionNumber), 0) FROM RouteRevisionEntity r WHERE r.routeId = :routeId")
    int findMaxRevisionNumber(@Param("routeId") UUID routeId);
}
