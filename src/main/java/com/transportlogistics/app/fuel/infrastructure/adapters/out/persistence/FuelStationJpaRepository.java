package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FuelStationJpaRepository extends JpaRepository<FuelStationEntity, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    List<FuelStationEntity> findByActiveOrderByNameAsc(boolean active);
}
