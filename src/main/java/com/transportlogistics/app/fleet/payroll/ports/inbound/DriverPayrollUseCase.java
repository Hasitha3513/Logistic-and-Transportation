package com.transportlogistics.app.fleet.payroll.ports.inbound;
import com.transportlogistics.app.fleet.payroll.domain.*;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollStore;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public interface DriverPayrollUseCase {
 DriverPayrollInputBatch create(Context c, Create x, String idempotencyKey);
 List<DriverPayrollInputBatch> list(UUID tenantId,int page,int size);
 DriverPayrollInputBatch get(UUID tenantId,UUID id);
 DriverPayrollInputBatch replaceLines(Context c,UUID id,long version,List<LineCommand> lines);
 DriverPayrollInputBatch validate(Context c,UUID id,long version);
 DriverPayrollInputBatch approve(Context c,UUID id,long version);
 DriverPayrollInputBatch export(Context c,UUID id,long version);
 DriverPayrollInputBatch correction(Context c,UUID id,CreateCorrection x,String idempotencyKey);
 List<DriverPayrollStore.History> history(UUID tenantId,UUID id);
 Optional<DriverPayrollWorkerMapping> mapping(UUID tenantId,UUID driverId);
 DriverPayrollWorkerMapping mapWorker(Context c,UUID driverId,MappingCommand x,String idempotencyKey);
 record Context(UUID tenantId,UUID actorId,String username,String correlationId){}
 record Create(DriverPayrollInputBatch.Type type,UUID correctionOfBatchId,LocalDate periodStart,
               LocalDate periodEndExclusive,OffsetDateTime cutoffAt,String currency){}
 record LineCommand(UUID id,UUID driverId,UUID tripId,String tripNumber,DriverPayrollInputLine.Category category,
                    String reasonCode,String description,BigDecimal quantity,DriverPayrollInputLine.Unit unit,
                    BigDecimal rate,BigDecimal amount,UUID originalLineId){}
 record CreateCorrection(long version,LocalDate periodStart,LocalDate periodEndExclusive,OffsetDateTime cutoffAt,
                         String currency,List<LineCommand> lines){}
 record MappingCommand(String externalSystemAlias,String externalWorkerReference,boolean active,long version){}
}
