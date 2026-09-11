package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.TrackingModels.Association;
import com.transportlogistics.app.tracking.domain.TrackingModels.Device;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Observation;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.State;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TrackingUseCase {
 Device create(Context context,CreateDevice command);
 Device update(Context context,UUID id,long version,UpdateDevice command);
 Device lifecycle(Context context,UUID id,long version,DeviceLifecycle lifecycle);
 Device get(UUID tenantId,UUID id,boolean revealReferences);
 List<Device> devices(UUID tenantId,int page,int size,boolean revealReferences);
 Association associate(Context context,UUID deviceId,Associate command);
 Association endAssociation(Context context,UUID deviceId,UUID associationId,Instant effectiveTo);
 Optional<Association> activeAssociation(UUID tenantId,UUID deviceId);
 List<State> vehicles(UUID tenantId,int page,int size,Instant now);
 State latest(UUID tenantId,UUID vehicleId,Instant now);
 HistoryPage positions(UUID tenantId,UUID vehicleId,Instant from,Instant to,String cursor,int limit);
 List<IngestResult> ingest(ProviderContext provider,List<PositionCommand> positions,Instant receivedAt);
 record Context(UUID tenantId,UUID actorId,String correlationId) {}
 record ProviderContext(UUID tenantId,String providerAlias) {}
 record CreateDevice(String externalDeviceReference,String providerAlias,String hardwareSerialReference) {}
 record UpdateDevice(String externalDeviceReference,String hardwareSerialReference) {}
 record Associate(UUID vehicleId,Instant effectiveFrom) {}
 record PositionCommand(UUID deviceId,String providerMessageId,Long providerSequence,Instant sourceTimestamp,BigDecimal latitude,BigDecimal longitude,BigDecimal horizontalAccuracyMeters,BigDecimal speedKph,BigDecimal headingDegrees,BigDecimal altitudeMeters,EngineState engineState,BigDecimal odometerKm,BigDecimal engineHours,Map<String,String> safeMetadata) {}
 record IngestResult(UUID positionId,UUID vehicleId,String dedupeIdentity,boolean duplicate,Trust trust,Ordering ordering) {}
 record HistoryPage(List<Observation> items,String nextCursor) {}
}
