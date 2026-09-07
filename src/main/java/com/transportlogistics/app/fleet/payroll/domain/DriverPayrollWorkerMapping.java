package com.transportlogistics.app.fleet.payroll.domain;
import java.time.OffsetDateTime;
import java.util.UUID;
public record DriverPayrollWorkerMapping(UUID id, UUID tenantId, UUID driverId, String externalSystemAlias,
                                         String externalWorkerReference, boolean active, long version,
                                         UUID updatedBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
