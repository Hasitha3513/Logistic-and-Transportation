package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface VendorJpaRepository extends JpaRepository<VendorEntity, UUID> {
    boolean existsByCodeAndIdNot(String code, UUID id);

    List<VendorEntity> findAllByActiveOrderByName(boolean active);

    List<VendorEntity> findAllByOrderByName();
}
