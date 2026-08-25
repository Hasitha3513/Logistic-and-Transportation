package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.domain.model.DisruptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RouteDisruptionJpaRepository extends JpaRepository<RouteDisruptionEntity, UUID> {
    List<RouteDisruptionEntity> findByRouteIdOrderByCreatedAtDesc(UUID routeId);

    List<RouteDisruptionEntity> findByStatusOrderByCreatedAtDesc(DisruptionStatus status);
}
