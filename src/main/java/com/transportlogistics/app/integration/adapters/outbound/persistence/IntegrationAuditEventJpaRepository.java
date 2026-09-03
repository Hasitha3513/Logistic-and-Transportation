package com.transportlogistics.app.integration.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface IntegrationAuditEventJpaRepository extends JpaRepository<IntegrationAuditEventEntity, UUID> {}
