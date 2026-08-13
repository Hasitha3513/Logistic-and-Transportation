package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface LocationJpaRepository extends JpaRepository<LocationEntity, UUID> {
}
