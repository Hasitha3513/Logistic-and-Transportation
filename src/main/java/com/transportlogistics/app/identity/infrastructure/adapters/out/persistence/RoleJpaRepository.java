package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
}