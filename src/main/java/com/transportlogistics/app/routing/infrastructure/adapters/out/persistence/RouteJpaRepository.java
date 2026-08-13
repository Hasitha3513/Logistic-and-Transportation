package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RouteJpaRepository extends JpaRepository<RouteEntity, UUID> {
}
