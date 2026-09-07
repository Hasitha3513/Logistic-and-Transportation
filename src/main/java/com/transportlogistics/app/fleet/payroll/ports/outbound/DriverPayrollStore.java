package com.transportlogistics.app.fleet.payroll.ports.outbound;
import com.transportlogistics.app.fleet.payroll.domain.*;
import java.time.OffsetDateTime;
import java.util.*;
public interface DriverPayrollStore {
 Optional<DriverPayrollInputBatch> find(UUID tenantId, UUID id);
 Optional<DriverPayrollInputBatch> findByIdempotency(UUID tenantId, String key);
 List<DriverPayrollInputBatch> list(UUID tenantId, int page, int size);
 DriverPayrollInputBatch insert(DriverPayrollInputBatch batch, String key);
 void lockBatchCommand(UUID tenantId, String idempotencyKey);
 void lockReleasedSource(UUID tenantId, UUID driverId, UUID tripId, String category, UUID originalLineId);
 DriverPayrollInputBatch update(DriverPayrollInputBatch batch, long expectedVersion);
 Optional<DriverPayrollWorkerMapping> mapping(UUID tenantId, UUID driverId);
 DriverPayrollWorkerMapping saveMapping(DriverPayrollWorkerMapping mapping, long expectedVersion);
 Optional<MappingCommandResult> mappingCommand(UUID tenantId, String idempotencyKey);
 MappingCommandResult saveMappingCommand(UUID tenantId, UUID driverId, String idempotencyKey,
                                         String requestHash, DriverPayrollWorkerMapping result,
                                         UUID actorId, OffsetDateTime createdAt);
 void lockMappingCommand(UUID tenantId, String idempotencyKey);
 Optional<DriverPayrollInputBatch> findByExportEvent(UUID tenantId, UUID exportEventId);
 boolean releasedSourceExists(UUID tenantId, UUID driverId, UUID tripId, String category,
                              UUID originalLineId, UUID excludingBatch);
 boolean originalLineBelongsToBatch(UUID tenantId, UUID originalLineId, UUID batchId);
 List<History> history(UUID tenantId, UUID batchId);
 void history(UUID tenantId, UUID batchId, String action, String fromState, String toState, UUID actor,
              String detail, OffsetDateTime at);
 record History(UUID id,String action,String fromState,String toState,UUID actorId,String detail,OffsetDateTime createdAt){}
 record MappingCommandResult(String requestHash, DriverPayrollWorkerMapping mapping) {}
}
