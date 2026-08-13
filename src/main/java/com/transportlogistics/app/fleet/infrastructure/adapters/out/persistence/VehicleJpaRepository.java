package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {
}
