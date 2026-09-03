package com.transportlogistics.app.operations.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AssignmentHistoryJpaRepository extends JpaRepository<AssignmentHistoryEntity, UUID> {}
