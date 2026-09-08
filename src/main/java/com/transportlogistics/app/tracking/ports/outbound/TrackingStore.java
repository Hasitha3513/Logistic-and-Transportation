package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.TrackingModels.Association;
import com.transportlogistics.app.tracking.domain.TrackingModels.Device;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.State;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Associate;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.CreateDevice;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.HistoryPage;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.IngestResult;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.PositionCommand;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.ProviderContext;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.UpdateDevice;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingStore {
 Device insertDevice(Context context,CreateDevice command,Instant now);
 Device updateDevice(Context context,UUID id,long version,UpdateDevice command,Instant now);
 Device lifecycle(Context context,UUID id,long version,DeviceLifecycle lifecycle,Instant now);
 Optional<Device> device(UUID tenantId,UUID id);
 List<Device> devices(UUID tenantId,int page,int size);
 Association associate(Context context,UUID deviceId,Associate command,Instant now);
 Association endAssociation(Context context,UUID deviceId,UUID associationId,Instant effectiveTo,Instant now);
 List<State> states(UUID tenantId,int page,int size,Instant now);
 Optional<State> state(UUID tenantId,UUID vehicleId,Instant now);
 HistoryPage positions(UUID tenantId,UUID vehicleId,Instant from,Instant to,String cursor,int limit);
 List<IngestResult> ingest(ProviderContext provider,List<PositionCommand> commands,Instant receivedAt);
 boolean reserveNonce(UUID tenantId,String providerAlias,String nonceHash,Instant now,Instant expiresAt);
}
