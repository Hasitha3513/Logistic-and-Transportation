package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TripJpaRepository extends JpaRepository<TripEntity, UUID> {
}